// Copyright 2017 Google Inc. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

'use strict';

var audio = null;  // global audio context

class Audio {
    constructor(synthUnit, sendMidi) {
        this._synthUnit = synthUnit;
        this._sendMidi = sendMidi;
    }

    sendMidi(data) {
        var midiBuf = Module._malloc(data.length);
        Module.writeArrayToMemory(data, midiBuf);
        this._sendMidi(this._synthUnit, midiBuf, data.length);
        Module._free(midiBuf);
    }
}

function start() {
    var ctx = new AudioContext();

    // Initialize the emscripten core
    var sampleRate = ctx.sampleRate;
    var synthUnit = Module.ccall('synth_create', 'number', ['number'], [sampleRate]);
    console.log('synth pointer =', synthUnit);
    var getSamples = Module.cwrap('synth_get_samples', null, ['number', 'number', 'number']);
    var sendMidi = Module.cwrap('synth_send_midi', null, ['number', 'number', 'number']);

    var scriptNode = ctx.createScriptProcessor(256, 0, 1);
    var bufSize = scriptNode.bufferSize;
    var xferBuf = Module._malloc(bufSize * 2);
    scriptNode.onaudioprocess = function(audioProcessingEvent) {
        //console.log(audioProcessingEvent);
        getSamples(synthUnit, bufSize, xferBuf);
        var buf = audioProcessingEvent.outputBuffer.getChannelData(0);
        for (var i = 0; i < bufSize; i++) {
            buf[i] = Module.getValue(xferBuf + i*2, 'i16') * (1.0/32768);
        }
    }
    scriptNode.connect(ctx.destination);

    audio = new Audio(synthUnit, sendMidi);

    var midiAccess = null;
    var midiIn = null;

    function midiStateChange() {
        if (midiIn != null) {
            midiIn.onmidimessage = null;
        }
        var inputs = midiAccess.inputs.values();
        for (var input = inputs.next(); input && !input.done; input = inputs.next()) {
            // we just always take the first one in the list; could be more
            // sophisticated but whatevs.
            midiIn = input.value;
            break;
        }
        if (midiIn != null) {
            midiIn.onmidimessage = onMIDIMessage;
        }
    }

    function onMIDIMessage(message) {
        var data = message.data;
        console.log('midi:', data);
        if (data[0] == 0x90 || data[0] == 0x80) {
            var shiftedNote = data[1] - 48;
            var note = notes[shiftedNote];
            if (note != null && data[0] == 0x90) {
                note.note_on();
            }
            if (note != null && data[0] == 0x80) {
                note.note_off();
            }
        }
        audio.sendMidi(data);
    }

    function onMIDIStarted(midi) {
        midiAccess = midi;
        midi.onstatechange = midiStateChange;
        midiStateChange();
    }

    function onMIDISystemError(err) {
        console.log("MIDI error: " + err);
    }

    // MIDI
    navigator.requestMIDIAccess().then( onMIDIStarted, onMIDISystemError );
}

function audioInit() {
    if (window.emscriptenRuntimeReady) {
        start();
    } else {
        window.onEmscriptenRuntimeReady = start;
    }
}

window.addEventListener('load', audioInit);
