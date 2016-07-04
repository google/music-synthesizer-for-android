/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.levien.synthesizer.core.midi;

/**
 * Duplicate each MIDI message to two listeners
 */
public class MessageTee implements MidiListener {
  public MessageTee(MidiListener target) {
    target_ = target;
  }

  public void setSecondTarget(MidiListener target) {
    target2_ = target;
  }

  // Control events.
  public void onNoteOff(int channel, int note, int velocity) {
    target_.onNoteOff(channel, note, velocity);
    if (target2_ != null) {
      target2_.onNoteOff(channel, note, velocity);
    }
  }

  public void onNoteOn(int channel, int note, int velocity) {
    target_.onNoteOn(channel, note, velocity);
    if (target2_ != null) {
      target2_.onNoteOn(channel, note, velocity);
    }
  }

  public void onNoteAftertouch(int channel, int note, int aftertouch) {
    target_.onNoteAftertouch(channel, note, aftertouch);
    if (target2_ != null) {
      target2_.onNoteAftertouch(channel, note, aftertouch);
    }
  }

  public void onController(int channel, int control, int value) {
    target_.onController(channel, control, value);
    if (target2_ != null) {
      target2_.onController(channel, control, value);
    }
  }

  public void onProgramChange(int channel, int program) {
    target_.onProgramChange(channel, program);
    if (target2_ != null) {
      target2_.onProgramChange(channel, program);
    }
  }

  public void onChannelAftertouch(int channel, int aftertouch) {
    target_.onChannelAftertouch(channel, aftertouch);
    if (target2_ != null) {
      target2_.onChannelAftertouch(channel, aftertouch);
    }
  }

  public void onPitchBend(int channel, int value) {
    target_.onPitchBend(channel, value);
    if (target2_ != null) {
      target2_.onPitchBend(channel, value);
    }
  }

  // Other events.
  public void onTimingClock() {
    target_.onTimingClock();
    if (target2_ != null) {
      target2_.onTimingClock();
    }
  }

  public void onActiveSensing() {
    target_.onActiveSensing();
    if (target2_ != null) {
      target2_.onActiveSensing();
    }
  }

  // Meta events.
  public void onSequenceNumber(int sequenceNumber) {
    target_.onSequenceNumber(sequenceNumber);
    if (target2_ != null) {
      target2_.onSequenceNumber(sequenceNumber);
    }
  }

  public void onText(byte[] text) {
    target_.onText(text);
    if (target2_ != null) {
      target2_.onText(text);
    }
  }

  public void onCopyrightNotice(byte[] text) {
    target_.onCopyrightNotice(text);
    if (target2_ != null) {
      target2_.onCopyrightNotice(text);
    }
  }

  public void onSequenceName(byte[] text) {
    target_.onSequenceName(text);
    if (target2_ != null) {
      target2_.onSequenceName(text);
    }
  }

  public void onInstrumentName(byte[] text) {
    target_.onInstrumentName(text);
    if (target2_ != null) {
      target2_.onInstrumentName(text);
    }
  }

  public void onLyrics(byte[] text) {
    target_.onLyrics(text);
    if (target2_ != null) {
      target2_.onLyrics(text);
    }
  }

  public void onMarker(byte[] text) {
    target_.onMarker(text);
    if (target2_ != null) {
      target2_.onMarker(text);
    }
  }

  public void onCuePoint(byte[] text) {
    target_.onCuePoint(text);
    if (target2_ != null) {
      target2_.onCuePoint(text);
    }
  }

  public void onChannelPrefix(int channel) {
    target_.onChannelPrefix(channel);
    if (target2_ != null) {
      target2_.onChannelPrefix(channel);
    }
  }

  public void onPort(byte[] data) {
    target_.onPort(data);
    if (target2_ != null) {
      target2_.onPort(data);
    }
  }

  public void onEndOfTrack() {
    target_.onEndOfTrack();
    if (target2_ != null) {
      target2_.onEndOfTrack();
    }
  }

  public void onSetTempo(int microsecondsPerQuarterNote) {
    target_.onSetTempo(microsecondsPerQuarterNote);
    if (target2_ != null) {
      target2_.onSetTempo(microsecondsPerQuarterNote);
    }
  }

  public void onSmpteOffset(byte[] data) {
    target_.onSmpteOffset(data);
    if (target2_ != null) {
      target2_.onSmpteOffset(data);
    }
  }

  public void onTimeSignature(int numerator,
                       int denominator,
                       int metronomePulse,
                       int thirtySecondNotesPerQuarterNote) {
    target_.onTimeSignature(numerator, denominator, metronomePulse,
            thirtySecondNotesPerQuarterNote);
    if (target2_ != null) {
      target2_.onTimeSignature(numerator, denominator, metronomePulse,
              thirtySecondNotesPerQuarterNote);
    }
  }

  public void onKeySignature(int key, boolean isMinor) {
    target_.onKeySignature(key, isMinor);
    if (target2_ != null) {
      target2_.onKeySignature(key, isMinor);
    }
  }

  public void onSequencerSpecificEvent(byte[] data) {
    target_.onSequencerSpecificEvent(data);
    if (target2_ != null) {
      target2_.onSequencerSpecificEvent(data);
    }
  }

  // SysEx events.
  public void onSysEx(byte[] data) {
    target_.onSysEx(data);
    if (target2_ != null) {
      target2_.onSysEx(data);
    }
  }

  private final MidiListener target_;
  private MidiListener target2_;
}
