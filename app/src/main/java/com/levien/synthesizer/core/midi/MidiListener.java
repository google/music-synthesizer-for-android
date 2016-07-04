/*
 * Copyright 2011 Google Inc.
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
 * An interface for listening for any kind of midi event.
 * @see MidiAdapter
 */
public interface MidiListener {
  // Control events.
  void onNoteOff(int channel, int note, int velocity);
  void onNoteOn(int channel, int note, int velocity);
  void onNoteAftertouch(int channel, int note, int aftertouch);
  void onController(int channel, int control, int value);
  void onProgramChange(int channel, int program);
  void onChannelAftertouch(int channel, int aftertouch);
  void onPitchBend(int channel, int value);

  // Other events.
  void onTimingClock();
  void onActiveSensing();

  // Meta events.
  // The events that have "byte[] text" are _probably_ encoded as ISO-8859-1.
  void onSequenceNumber(int sequenceNumber);
  void onText(byte[] text);
  void onCopyrightNotice(byte[] text);
  void onSequenceName(byte[] text);
  void onInstrumentName(byte[] text);
  void onLyrics(byte[] text);
  void onMarker(byte[] text);
  void onCuePoint(byte[] text);
  void onChannelPrefix(int channel);
  void onPort(byte[] data);  // TODO(klimt):  Decode this.
  void onEndOfTrack();
  void onSetTempo(int microsecondsPerQuarterNote);
  void onSmpteOffset(byte[] data);  // TODO(klimt):  Decode this.
  void onTimeSignature(int numerator,
                       int denominator,
                       int metronomePulse,
                       int thirtySecondNotesPerQuarterNote);
  void onKeySignature(int key, boolean isMinor);
  void onSequencerSpecificEvent(byte[] data);

  // SysEx events.
  void onSysEx(byte[] data);
}
