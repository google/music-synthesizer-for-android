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
    buffer_.write(0x80 | channel);
    buffer_.write(note);
    buffer_.write(velocity);
    notifyMessage();
  }

  public void onNoteOn(int channel, int note, int velocity) {
    buffer_.write(0x90 | channel);
    buffer_.write(note);
    buffer_.write(velocity);
    notifyMessage();
  }

  public void onNoteAftertouch(int channel, int note, int aftertouch) {
    buffer_.write(0xA0 | channel);
    buffer_.write(note);
    buffer_.write(aftertouch);
    notifyMessage();
  }

  public void onController(int channel, int control, int value) {
    buffer_.write(0xB0 | channel);
    buffer_.write(control);
    buffer_.write(value);
    notifyMessage();
  }

  public void onProgramChange(int channel, int program) {
    buffer_.write(0xC0 | channel);
    buffer_.write(program);
    notifyMessage();
  }

  public void onChannelAftertouch(int channel, int aftertouch) {
    buffer_.write(0xD0 | channel);
    buffer_.write(aftertouch);
    notifyMessage();
  }

  public void onPitchBend(int channel, int value) {
    try {
      buffer_.write(0xE0 | channel);
      MidiUtil.writeWord(buffer_, value);
      notifyMessage();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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
    try {
      buffer_.write(0xFF);
      buffer_.write(0x01);
      MidiUtil.writeVarInt(buffer_, text.length);
      buffer_.write(text);
      notifyMessage();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void onCopyrightNotice(byte[] text) {
    try {
      buffer_.write(0xFF);
      buffer_.write(0x02);
      MidiUtil.writeVarInt(buffer_, text.length);
      buffer_.write(text);
      notifyMessage();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void onSequenceName(byte[] text) {
    try {
      buffer_.write(0xFF);
      buffer_.write(0x03);
      MidiUtil.writeVarInt(buffer_, text.length);
      buffer_.write(text);
      notifyMessage();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void onInstrumentName(byte[] text) {
    try {
      buffer_.write(0xFF);
      buffer_.write(0x04);
      MidiUtil.writeVarInt(buffer_, text.length);
      buffer_.write(text);
      notifyMessage();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void onLyrics(byte[] text) {
    try {
      buffer_.write(0xFF);
      buffer_.write(0x05);
      MidiUtil.writeVarInt(buffer_, text.length);
      buffer_.write(text);
      notifyMessage();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void onMarker(byte[] text) {
    try {
      buffer_.write(0xFF);
      buffer_.write(0x06);
      MidiUtil.writeVarInt(buffer_, text.length);
      buffer_.write(text);
      notifyMessage();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void onCuePoint(byte[] text) {
    try {
      buffer_.write(0xFF);
      buffer_.write(0x07);
      MidiUtil.writeVarInt(buffer_, text.length);
      buffer_.write(text);
      notifyMessage();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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
    try {
      buffer_.write(0xFF);
      buffer_.write(0x21);
      MidiUtil.writeVarInt(buffer_, data.length);
      buffer_.write(data);
      notifyMessage();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void onEndOfTrack() {
    try {
      buffer_.write(0xFF);
      buffer_.write(0x2F);
      MidiUtil.writeVarInt(buffer_, 0);
      notifyMessage();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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
    try {
      buffer_.write(0xFF);
      buffer_.write(0x54);
      MidiUtil.writeVarInt(buffer_, 5);
      buffer_.write(data);
      notifyMessage();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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
    try {
      buffer_.write(0xFF);
      buffer_.write(0x7F);
      MidiUtil.writeVarInt(buffer_, data.length);
      buffer_.write(data);
      notifyMessage();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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

  // An internal byte buffer to hold intermediate output.
  private ByteArrayOutputStream buffer_;
}
