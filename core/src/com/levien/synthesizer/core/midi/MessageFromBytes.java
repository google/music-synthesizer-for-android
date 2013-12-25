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
 * MessageFromBytes sends MIDI messages from bytes to a MidiListener.
 */
public class MessageFromBytes {
  public static final int ERROR = -1;
  public static final int NEEDMOREBYTES = -2;

  /**
   * Send a midi message to the listener. Note: the message must begin
   * with a valid status byte (running status is not supported).
   *
   * @param l the MidiListener to receive the message
   * @param b the data
   * @param off the start offset in the data
   * @param len the number of bytes to consume
   * @return Number of bytes consumed (nonnegative) on success, or
   * ERROR if it's not valid MIDI, or NEEDMOREBYTES if it's truncated
   */
  public static int send(MidiListener l, byte[] b, int off, int len) {
    if (len == 0) {
      return 0;
    }
    int code = b[off] & 0xff;
    if ((code & 0xf0) == 0xf0) {
      // TODO: NYI
      return ERROR;
    } else if ((code & 0x80) == 0x80) {
      int channel = code & 0x0f;
      int nbytes = ((code & 0xe0) == 0xc0) ? 2 : 3;
      if (len < nbytes) {
        return NEEDMOREBYTES;
      }
      if ((b[off + 1] & 0x80) != 0 || nbytes > 2 && (b[off + 2] & 0x80) != 0) {
        return ERROR;
      }
      switch (code & 0xf0) {
        case 0x80:
          l.onNoteOff(channel, b[off + 1], b[off + 2]);
          break;
        case 0x90:
          l.onNoteOn(channel, b[off + 1], b[off + 2]);
          break;
        case 0xa0:
          l.onNoteAftertouch(channel, b[off + 1], b[off + 2]);
          break;
        case 0xb0:
          l.onController(channel, b[off + 1], b[off + 2]);
          break;
        case 0xc0:
          l.onProgramChange(channel, b[off + 1]);
          break;
        case 0xd0:
          l.onChannelAftertouch(channel, b[off + 1]);
          break;
        case 0xe0:
          l.onPitchBend(channel, b[off + 1] + (b[off + 2] << 7));
          break;
      }
      return nbytes;
    }
    return ERROR;
  }

  public static int send(MidiListener l, byte[] b) {
    return send(l, b, 0, b.length);
  }

  /**
   * Send a sequence of midi messages to the listener. Note: each message must
   * begin with a valid status byte (running status is not supported).
   *
   * @param l the MidiListener to receive the message
   * @param b the data
   * @param off the start offset in the data
   * @param len the number of bytes to consume
   * @return Number of bytes consumed (nonnegative) on success, or
   * ERROR if it's not valid MIDI, or NEEDMOREBYTES if it's truncated
   */
  public static int sendAll(MidiListener l, byte[] b, int off, int len) {
    int i = 0;
    while (i < len) {
      int result = send(l, b, off + i, len - i);
      if (result < 0) {
        if (i == 0) {
          return result;
        } else {
          break;
        }
      }
      i += result;
    }
    return i;
  }

  public static int sendAll(MidiListener l, byte[] b) {
    return sendAll(l, b, 0, b.length);
  }
}
