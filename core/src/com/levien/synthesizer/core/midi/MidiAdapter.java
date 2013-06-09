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
 * An implementation of MidiListener with stub implementations so that clients don't have to handle
 * every single event.
 * @see MidiListener
 */
public class MidiAdapter implements MidiListener {
  public void onNoteOff(int channel, int note, int velocity) {}
  public void onNoteOn(int channel, int note, int velocity) {}
  public void onNoteAftertouch(int channel, int note, int aftertouch) {}
  public void onController(int channel, int control, int value) {}
  public void onProgramChange(int channel, int program) {}
  public void onChannelAftertouch(int channel, int aftertouch) {}
  public void onPitchBend(int channel, int value) {}

  public void onTimingClock() {}
  public void onActiveSensing() {}

  public void onSequenceNumber(int sequenceNumber) {}
  public void onText(byte[] text) {}
  public void onCopyrightNotice(byte[] text) {}
  public void onSequenceName(byte[] text) {}
  public void onInstrumentName(byte[] text) {}
  public void onLyrics(byte[] text) {}
  public void onMarker(byte[] text) {}
  public void onCuePoint(byte[] text) {}
  public void onChannelPrefix(int channel) {}
  public void onPort(byte[] data) {}
  public void onEndOfTrack() {}
  public void onSetTempo(int microsecondsPerQuarterNote) {}
  public void onSmpteOffset(byte[] data) {}
  public void onTimeSignature(int numerator,
                              int denominator,
                              int metronomePulse,
                              int thirtySecondNotesPerQuarterNote) {}
  public void onKeySignature(int key, boolean isMinor) {}
  public void onSequencerSpecificEvent(byte[] data) {}

  public void onSysEx(byte[] data) {}
}
