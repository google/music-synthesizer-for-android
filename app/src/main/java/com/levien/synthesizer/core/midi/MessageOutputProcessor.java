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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * MessageOutputProcessor listens for midi events and writes calls onMessage() with the binary
 * form of each one.
 * @see MidiListener
 */
public abstract class MessageOutputProcessor implements MidiListener {
  /**
   * Creates a new MessageOutputProcessor.
   */
  public MessageOutputProcessor() {
    buffer_ = new ByteArrayOutputStream();
  }

  /**
   * Called for each new midi event.
   * @param message - The midi message in bytes.
   */
  protected abstract void onMessage(byte[] message);

  /**
   * Internal function called to flush the internal byte buffer to onMessage().
   */
  private void notifyMessage() {
    onMessage(buffer_.toByteArray());
    buffer_.reset();
  }

  //
  // The rest of these methods are just midi listener events.
  //

  public void onNoteOff(int channel, int note, int velocity) {
    notify3(0x80 | channel, note, velocity);
  }

  public void onNoteOn(int channel, int note, int velocity) {
    notify3(0x90 | channel, note, velocity);
  }

  public void onNoteAftertouch(int channel, int note, int aftertouch) {
    notify3(0xA0 | channel, note, aftertouch);
  }

  public void onController(int channel, int control, int value) {
    notify3(0xB0 | channel, control, value);
  }

  public void onProgramChange(int channel, int program) {
    notify2(0xC0 | channel, program);
  }

  public void onChannelAftertouch(int channel, int aftertouch) {
    notify2(0xD0 | channel, aftertouch);
  }

  public void onPitchBend(int channel, int value) {
    notify3(0xE0 | channel, value & 0x7F, (value >> 7) & 0x7F);
  }

  public void onTimingClock() {
    buffer_.write(0xF8);
  }

  public void onActiveSensing() {
    buffer_.write(0xFE);
  }

  public void onSequenceNumber(int sequenceNumber) {
    try {
      buffer_.write(0xFF);
      buffer_.write(0x00);
      MidiUtil.writeVarInt(buffer_, 2);
      MidiUtil.writeWord(buffer_, sequenceNumber);
      notifyMessage();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void onText(byte[] text) {
    notifyMetaBytes(0x01, text);
  }

  public void onCopyrightNotice(byte[] text) {
    notifyMetaBytes(0x02, text);
  }

  public void onSequenceName(byte[] text) {
    notifyMetaBytes(0x03, text);
  }

  public void onInstrumentName(byte[] text) {
    notifyMetaBytes(0x04, text);
  }

  public void onLyrics(byte[] text) {
    notifyMetaBytes(0x05, text);
  }

  public void onMarker(byte[] text) {
    notifyMetaBytes(0x05, text);
  }

  public void onCuePoint(byte[] text) {
    notifyMetaBytes(0x07, text);
  }

  public void onChannelPrefix(int channel) {
    try {
      buffer_.write(0xFF);
      buffer_.write(0x20);
      MidiUtil.writeVarInt(buffer_, 1);
      buffer_.write(channel);
      notifyMessage();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void onPort(byte[] data) {
    notifyMetaBytes(0x21, data);
  }

  public void onEndOfTrack() {
    notify3(0xFF, 0x2F, 0x00);
  }

  public void onSetTempo(int microsecondsPerQuarterNote) {
    try {
      buffer_.write(0xFF);
      buffer_.write(0x51);
      MidiUtil.writeVarInt(buffer_, 3);
      MidiUtil.writeWord(buffer_, (microsecondsPerQuarterNote >> 8) & 0xFFFF);
      buffer_.write(microsecondsPerQuarterNote & 0xFF);
      notifyMessage();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void onSmpteOffset(byte[] data) {
    if (data.length != 5) {
      throw new RuntimeException("Invalid length for smpte offset event " + data.length + ".");
    }
    notifyMetaBytes(0x54, data);
  }

  public void onTimeSignature(int numerator, int denominator, int metronomePulse,
                              int thirtySecondNotesPerQuarterNote) {
    try {
      buffer_.write(0xFF);
      buffer_.write(0x58);
      MidiUtil.writeVarInt(buffer_, 4);
      buffer_.write(numerator);
      buffer_.write(denominator);
      buffer_.write(metronomePulse);
      buffer_.write(thirtySecondNotesPerQuarterNote);
      notifyMessage();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void onKeySignature(int key, boolean isMinor) {
    try {
      buffer_.write(0xFF);
      buffer_.write(0x59);
      MidiUtil.writeVarInt(buffer_, 2);
      buffer_.write(key);
      buffer_.write(isMinor ? 1 : 0);
      notifyMessage();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void onSequencerSpecificEvent(byte[] data) {
    notifyMetaBytes(0x7f, data);
  }

  /**
   * TODO(klimt): This might be wrong.  Double-check it. :)
   */
  public void onSysEx(byte[] data) {
    try {
      buffer_.write(0xF0);
      MidiUtil.writeVarInt(buffer_, data.length);
      buffer_.write(data);
      notifyMessage();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void notify2(int b0, int b1) {
    if (buf2_ == null) {
      buf2_ = new byte[2];
    }
    buf2_[0] = (byte) b0;
    buf2_[1] = (byte) b1;
    onMessage(buf2_);
  }

  private void notify3(int b0, int b1, int b2) {
    if (buf3_ == null) {
      buf3_ = new byte[3];
    }
    buf3_[0] = (byte) b0;
    buf3_[1] = (byte) b1;
    buf3_[2] = (byte) b2;
    onMessage(buf3_);
  }

  private void notifyMetaBytes(int type, byte[] data) {
    try {
      buffer_.write(0xFF);
      buffer_.write(type);
      MidiUtil.writeVarInt(buffer_, data.length);
      buffer_.write(data);
      notifyMessage();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  // An internal byte buffer to hold intermediate output.
  private ByteArrayOutputStream buffer_;
  private byte[] buf2_;
  private byte[] buf3_;
}
