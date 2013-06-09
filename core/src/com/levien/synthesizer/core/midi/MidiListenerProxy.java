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
 * A MidiListenerProxy is a MidiListener implementation that just passes all the events on to
 * another MidiListener.  It's really only useful if you want to subclass and override some of the
 * methods.
 * @see MidiListener
 */
public class MidiListenerProxy implements MidiListener {
  /**
   * Creates a new MidiListenerProxy that passes all its events on to listener.
   */
  public MidiListenerProxy(MidiListener listener) {
    listener_ = listener;
  }

  //
  // The rest of these methods are just implementations of MidiListener.
  //

  public void onNoteOff(int channel, int note, int velocity) {
    listener_.onNoteOff(channel, note, velocity);
  }

  public void onNoteOn(int channel, int note, int velocity) {
    listener_.onNoteOn(channel, note, velocity);
  }

  public void onNoteAftertouch(int channel, int note, int aftertouch) {
    listener_.onNoteAftertouch(channel, note, aftertouch);
  }

  public void onController(int channel, int control, int value) {
    listener_.onController(channel, control, value);
  }

  public void onProgramChange(int channel, int program) {
    listener_.onProgramChange(channel, program);
  }

  public void onChannelAftertouch(int channel, int aftertouch) {
    listener_.onChannelAftertouch(channel, aftertouch);
  }

  public void onPitchBend(int channel, int value) {
    listener_.onPitchBend(channel, value);
  }

  public void onTimingClock() {
    listener_.onTimingClock();
  }

  public void onActiveSensing() {
    listener_.onActiveSensing();
  }

  public void onSequenceNumber(int sequenceNumber) {
    listener_.onSequenceNumber(sequenceNumber);
  }

  public void onText(byte[] text) {
    listener_.onText(text);
  }

  public void onCopyrightNotice(byte[] text) {
    listener_.onCopyrightNotice(text);
  }

  public void onSequenceName(byte[] text) {
    listener_.onSequenceName(text);
  }

  public void onInstrumentName(byte[] text) {
    listener_.onInstrumentName(text);
  }

  public void onLyrics(byte[] text) {
    listener_.onLyrics(text);
  }

  public void onMarker(byte[] text) {
    listener_.onMarker(text);
  }

  public void onCuePoint(byte[] text) {
    listener_.onCuePoint(text);
  }

  public void onChannelPrefix(int channel) {
    listener_.onChannelPrefix(channel);
  }

  public void onPort(byte[] data) {
    listener_.onPort(data);
  }

  public void onEndOfTrack() {
    listener_.onEndOfTrack();
  }

  public void onSetTempo(int microsecondsPerQuarterNote) {
    listener_.onSetTempo(microsecondsPerQuarterNote);
  }

  public void onSmpteOffset(byte[] data) {
    listener_.onSmpteOffset(data);
  }

  public void onTimeSignature(int numerator,
                              int denominator,
                              int metronomePulse,
                              int thirtySecondNotesPerQuarterNote) {
    listener_.onTimeSignature(numerator, denominator, metronomePulse,
                              thirtySecondNotesPerQuarterNote);
  }

  public void onKeySignature(int key, boolean isMinor) {
    listener_.onKeySignature(key, isMinor);
  }

  public void onSequencerSpecificEvent(byte[] data) {
    listener_.onSequencerSpecificEvent(data);
  }

  public void onSysEx(byte[] data) {
    listener_.onSysEx(data);
  }

  // The listener to forward events to.
  protected MidiListener listener_;
}
