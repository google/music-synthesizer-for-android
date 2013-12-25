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

import java.io.IOException;
import java.io.InputStream;

/**
 * MessageInputProcessor takes Midi messages from an input stream and dispatches them to a
 * MidiListener.
 * @see MidiListener
 */
public class MessageInputProcessor {
  /**
   * Creates a new MessageInputProcessor.
   */
  public MessageInputProcessor() {
    previousCode_ = 0;
  }

  /**
   * Reads one Midi message from input and dispatches any events to a listener.
   */
  public void process(InputStream input, MidiListener listener) throws IOException {
    previousCode_ = process(input, previousCode_, listener);
  }

  /**
   * Reads one Midi message from input and dispatches any events to a listener.
   * @param input - The stream to read from.
   * @param previousCode - The previous message code in the stream, for "running status" encoding.
   * @param listener - The object to handle the event.
   */
  public static int process(InputStream input,
                            int previousCode,
                            MidiListener listener) throws IOException {
    if (!input.markSupported()) {
      throw new IOException("process() requires an InputStream that supports mark().");
    }

    input.mark(1);
    int code = MidiUtil.readByte(input);
    if ((code & 0x80) == 0 && previousCode != 0) {
      code = previousCode;
      input.reset();
    }

    if (code == 0xFF) {
      processMetaMessage(input, listener);
    } else if (code == 0xFE) {
      listener.onActiveSensing();
    } else if (code == 0xF8) {
      listener.onTimingClock();
    } else if (code == 0xF0 || code == 0xF7) {
      processSysExMessage(input, listener);
    } else if ((code & 0x80) == 0x80 && (code & 0xF0) != 0xF0) {
      processControlMessage(input, code, listener);
    } else {
      throw new IOException("Invalid midi event code " + code + ".");
    }
    return code;
  }

  /**
   * Processes control messages.
   * @param input - The stream to read from.
   * @param code - The code of the event, which has the type and the channel.
   * @param listener - The object to handle the event.
   */
  private static void processControlMessage(InputStream input,
                                            int code,
                                            MidiListener listener) throws IOException {
    int type = (code & 0xF0);
    int channel = (code & 0x0F);
    switch (type) {
      case 0x80: {
        int note = MidiUtil.readByte(input);
        int velocity = MidiUtil.readByte(input);
        listener.onNoteOff(channel, note, velocity);
        break;
      }
      case 0x90: {
        int note = MidiUtil.readByte(input);
        int velocity = MidiUtil.readByte(input);
        listener.onNoteOn(channel, note, velocity);
        break;
      }
      case 0xA0: {
        int note = MidiUtil.readByte(input);
        int aftertouch = MidiUtil.readByte(input);
        listener.onNoteAftertouch(channel, note, aftertouch);
        break;
      }
      case 0xB0: {
        int control = MidiUtil.readByte(input);
        int value = MidiUtil.readByte(input);
        listener.onController(channel, control, value);
        break;
      }
      case 0xC0: {
        int program = MidiUtil.readByte(input);
        listener.onProgramChange(channel, program);
        break;
      }
      case 0xD0: {
        int aftertouch = MidiUtil.readByte(input);
        listener.onChannelAftertouch(channel, aftertouch);
        break;
      }
      case 0xE0: {
        int lsb = MidiUtil.readByte(input);
        int msb = MidiUtil.readByte(input);
        int value = (msb << 7) | lsb;
        listener.onPitchBend(channel, value);
        break;
      }
      default: {
        throw new IOException("Invalid midi control message type " + type + ".");
      }
    }
  }

  /**
   * Processes meta messages.
   * @param input - The stream to read from.
   * @param listener - The object to handle the event.
   */
  private static void processMetaMessage(InputStream input,
                                         MidiListener listener) throws IOException {
    int type = MidiUtil.readByte(input);
    int size = MidiUtil.readVarInt(input);
    switch (type) {
      case 0x00: {
        if (size != 2) {
          throw new IOException("Invalid length for sequence meta event " + size + ".");
        }
        listener.onSequenceNumber(MidiUtil.readWord(input));
        break;
      }
      case 0x01: {
        byte[] text = new byte[size];
        MidiUtil.readBytes(input, size, text);
        listener.onText(text);
        break;
      }
      case 0x02: {
        byte[] text = new byte[size];
        MidiUtil.readBytes(input, size, text);
        listener.onCopyrightNotice(text);
        break;
      }
      case 0x03: {
        byte[] text = new byte[size];
        MidiUtil.readBytes(input, size, text);
        listener.onSequenceName(text);
        break;
      }
      case 0x04: {
        byte[] text = new byte[size];
        MidiUtil.readBytes(input, size, text);
        listener.onInstrumentName(text);
        break;
      }
      case 0x05: {
        byte[] text = new byte[size];
        MidiUtil.readBytes(input, size, text);
        listener.onLyrics(text);
        break;
      }
      case 0x06: {
        byte[] text = new byte[size];
        MidiUtil.readBytes(input, size, text);
        listener.onMarker(text);
        break;
      }
      case 0x07: {
        byte[] text = new byte[size];
        MidiUtil.readBytes(input, size, text);
        listener.onCuePoint(text);
        break;
      }
      case 0x20: {
        if (size != 1) {
          throw new IOException("Invalid length for midi channel prefix " + size + ".");
        }
        listener.onChannelPrefix(MidiUtil.readByte(input));
        break;
      }
      case 0x21: {
        byte[] data = new byte[size];
        MidiUtil.readBytes(input, size, data);
        listener.onPort(data);
        break;
      }
      case 0x2F: {
        if (size != 0) {
          throw new IOException("Invalid length for end of track " + size + ".");
        }
        listener.onEndOfTrack();
        break;
      }
      case 0x51: {
        if (size != 3) {
          throw new IOException("Invalid length for set tempo event " + size + ".");
        }
        // Stupid 3-byte value.
        int w = MidiUtil.readWord(input);
        int b = MidiUtil.readByte(input);
        int mspqn = (w << 8) | b;
        listener.onSetTempo(mspqn);
        break;
      }
      case 0x54: {
        if (size != 5) {
          throw new IOException("Invalid length for smpte offset event " + size + ".");
        }
        byte[] data = new byte[size];
        MidiUtil.readBytes(input, size, data);
        listener.onSmpteOffset(data);
        break;
      }
      case 0x58: {
        if (size != 4) {
          throw new IOException("Invalid length for time signature event " + size + ".");
        }
        int numerator = MidiUtil.readByte(input);
        int denominator = MidiUtil.readByte(input);
        int metronomePulse = MidiUtil.readByte(input);
        int thirtySecondNotesPerQuarterNote = MidiUtil.readByte(input);
        listener.onTimeSignature(numerator,
                                 denominator,
                                 metronomePulse,
                                 thirtySecondNotesPerQuarterNote);
        break;
      }
      case 0x59: {
        if (size != 2) {
          throw new IOException("Invalid length for key signature event " + size + ".");
        }
        int key = MidiUtil.readByte(input);
        boolean isMinor = (MidiUtil.readByte(input) != 0);
        listener.onKeySignature(key, isMinor);
        break;
      }
      case 0x7F: {
        byte[] data = new byte[size];
        MidiUtil.readBytes(input, size, data);
        listener.onSequencerSpecificEvent(data);
        break;
      }
      default: {
        throw new IOException("Invalid midi meta message type " + type + ".");
      }
    }
  }

  /**
   * Processes SysEx messages.
   * @param input - The stream to read from.
   * @param listener - The object to handle the event.
   */
  private static void processSysExMessage(InputStream input,
                                          MidiListener listener) throws IOException {
    int size = MidiUtil.readVarInt(input);
    byte[] data = new byte[size];
    MidiUtil.readBytes(input, size, data);
    listener.onSysEx(data);
  }

  // The most recent code seen in the stream, used for "running status" encoding.
  int previousCode_;
}
