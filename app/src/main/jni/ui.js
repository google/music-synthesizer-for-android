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

var svgns = "http://www.w3.org/2000/svg";

var noteX = [0, 1, 2, 3, 4, 6, 7, 8, 9, 10, 11, 12];
var noteY = [0, 1, 0, 1, 0, 0, 1, 0, 1, 0,  1,  0];

var xscale = 25;
var yscale = 70;

var notes = {};

class Note {
    constructor(rect, cls) {
        this.rect = rect;
        this.cls = cls;
    }

    note_on() {
        this.rect.setAttribute("class", "pressed");
    }

    note_off() {
        this.rect.setAttribute("class", this.cls);
    }
}

function addNote(parent, noteNum) {
    var octave = Math.floor(noteNum / 12);
    var noteId = noteNum % 12;
    var x = 1 + xscale * (octave * 14 + noteX[noteId]);
    var y = 100 - yscale * noteY[noteId];
    var rect = document.createElementNS(svgns, "rect");
    rect.setAttribute("x", x);
    rect.setAttribute("y", y);
    rect.setAttribute("rx", 5);
    rect.setAttribute("ry", 5);
    rect.setAttribute("width", 2 * xscale - 2);
    rect.setAttribute("height", yscale - 2);
    var cls = noteY[noteId] ? "black" : "white";
    rect.setAttribute("class", cls);
    parent.appendChild(rect);
    rect.addEventListener('mouseover', function(e) {
        console.log('mouseover', noteNum);
    });
    rect.addEventListener('mousedown', function(e) {
        //e.target.setCapture();
        e.preventDefault();
        rect.setAttribute("class", "pressed");
        console.log(noteNum, 'down');
        audio.sendMidi(new Uint8Array([0x90, noteNum + 48, 64]));
        function upEvent(e) {
            e.preventDefault();
            rect.setAttribute("class", cls);
            console.log(noteNum, 'up');
            audio.sendMidi(new Uint8Array([0x80, noteNum + 48, 64]));
            document.removeEventListener('mouseup', upEvent);
        }
        document.addEventListener('mouseup', upEvent);
    });
    notes[noteNum] = new Note(rect, cls);
}

function uiInit() {
    var svg = document.getElementById('svg');
    var svgns = "http://www.w3.org/2000/svg";
    for (var note = 0; note < 25; note++) {
        addNote(svg, note);
    }
}

window.addEventListener('load', uiInit);
