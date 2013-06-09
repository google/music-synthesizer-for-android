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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * MidiReader is a set of static functions for reading midi data from a stream into the various
 * parts of a MidiFile.  You probably don't want to use these methods directly, but rather use
 * the constructor for MidiFile when reading a midi file.
 * @see MidiFile
 */
public class MidiReader {
  /**
   * Reads an entire midi file from input into file.
   * @param input - The input file to read from.
   * @param file - The object to store the data from the file in.
   * @throws IOException - On any kind of read error or invalid format.
   */
  public static void readMidiFile(InputStream input, MidiFile file) throws IOException {
    // This code makes use of mark() and reset(), so if that's not available, wrap the input with
    // a buffer that enables them.
    if (!input.markSupported()) {
      BufferedInputStream buffer = new BufferedInputStream(input);
      if (!buffer.markSupported()) {
        throw new IOException("BufferedInputStream does not support mark?");
      }
      readMidiFile(buffer, file);
      return;
    }

    readHeader(input, file.getHeader());
    input.mark(1);
    int b = input.read();
    while (b >= 0) {
      input.reset();
      readTrack(input, file.addTrack());
      input.mark(1);
      b = input.read();
    }
  }

  /**
   * Reads the header from a midi file and stores it in header.
   * @param input - The input file to read from.
   * @param header - The object to store the data in.
   * @throws IOException - On any kind of read error or invalid format.
   */
  private static void readHeader(InputStream input, MidiHeader header) throws IOException {
    verifyString(input, "MThd");
    if (MidiUtil.readDWord(input) != 6) {
      throw new IOException("Expected header size == 6.");
    }
    int formatNumber = MidiUtil.readWord(input);
    MidiHeader.Format format = MidiHeader.Format.valueOf(formatNumber);
    if (format == null) {
      throw new IOException("Invalid format " + formatNumber + ".");
    }
    header.setFormat(format);
    header.setTrackCount(MidiUtil.readWord(input));
    int timeDivision = MidiUtil.readWord(input);
    if ((timeDivision & 0x8000) == 0) {
      header.setTicksPerBeat(timeDivision & 0x7FFF);
    } else {
      header.setFramesPerSecond((timeDivision & 0x7F00) >> 16);
      header.setTicksPerFrame(timeDivision & 0xFF);
      throw new IOException("SMPTE time codes are not yet supported.");
    }
  }

  /**
   * Reads one track from a midi file and stores it in track.
   * @param input - The input file to read from.
   * @param track - The object to store the data in.
   * @throws IOException - On any kind of read error or invalid format.
   */
  private static void readTrack(InputStream input, MidiTrack track) throws IOException {
    verifyString(input, "MTrk");
    int size = MidiUtil.readDWord(input);
    byte[] trackBytes = new byte[size];
    MidiUtil.readBytes(input, size, trackBytes);
    ByteArrayInputStream trackStream = new ByteArrayInputStream(trackBytes);
    readEvents(trackStream, track);
    if (trackStream.read() >= 0) {
      throw new IOException("Unexpected data at end of track.");
    }
  }

  /**
   * Reads the events from a track in a midi file and stores it in track.
   * @param input - The input file to read from.
   * @param track - The object to store the data in.
   * @throws IOException - On any kind of read error or invalid format.
   */
  private static void readEvents(InputStream input, MidiTrack track) throws IOException {
    // This code makes use of mark() and reset(), so if that's not available, wrap the input with
    // a buffer that enables them.
    if (!input.markSupported()) {
      BufferedInputStream buffer = new BufferedInputStream(input);
      if (!buffer.markSupported()) {
        throw new IOException("BufferedInputStream does not support mark?");
      }
      readEvents(buffer, track);
      return;
    }

    int runningStatus = 0;
    input.mark(1);
    int b = input.read();
    while (b >= 0) {
      input.reset();
      runningStatus = readEvent(input, runningStatus, track.addEvent(new MidiEvent()));
      input.mark(1);
      b = input.read();
    }    
  }

  /**
   * Reads one event from a track in a midi file and stores it in event.
   * @param input - The input file to read from.
   * @param previousCode - The code byte from the most recent message read, for "Running Status".
   * @param event - The object to store the data in.
   * @throws IOException - On any kind of read error or invalid format.
   * @return The code for the read message, needed for "Running Status" decoding.
   */
  private static int readEvent(InputStream input,
                               int previousCode,
                               MidiEvent event) throws IOException {
    event.setDeltaTime(MidiUtil.readVarInt(input));
    return readMessage(input, previousCode, event);
  }

  /**
   * Reads one message from a track in a midi file and stores it in event.
   * @param input - The input file to read from.
   * @param previousCode - The code byte from the most recent message read, for "Running Status".
   * @param event - The object to store the data in.
   * @throws IOException - On any kind of read error or invalid format.
   * @return The code for the read message, needed for "Running Status" decoding.
   */
  private static int readMessage(InputStream input,
                                 int previousCode,
                                 MidiEvent event) throws IOException {
    // This code makes use of mark() and reset(), so if that's not available, wrap the input with
    // a buffer that enables them.
    if (!input.markSupported()) {
      BufferedInputStream buffer = new BufferedInputStream(input);
      if (!buffer.markSupported()) {
        throw new IOException("BufferedInputStream does not support mark?");
      }
      return readEvent(buffer, previousCode, event);
    }

    input.mark(1);
    int code = MidiUtil.readByte(input);
    if ((code & 0x80) == 0 && previousCode != 0) {
      code = previousCode;
      input.reset();
    }

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    output.write(code);

    if (code == 0xFF) {
      readMetaEvent(input, output);
    } else if (code == 0xF0 || code == 0xF7) {
      readSysExEvent(input, output);
    } else if ((code & 0x80) == 0x80 && (code & 0xF0) != 0xF0) {
      readControlEvent(input, code, output);
    } else {
      // This will only ever happen if either:
      //   (1) the first event doesn't have the first bit set, or
      //   (2) the code is in one of the ranges 0xF1-0xF6 or 0xF8-0xFE inclusive.
      throw new IOException("Invalid midi event code " + code + ".");
    }

    event.setMessage(output.toByteArray());
    return code;
  }

  /**
   * Copies one midi control message from input to output.
   * @param input - The input stream to read from.
   * @param code - The code byte of the message.
   * @param output - The output stream to write to.
   * @throws IOException - On any kind of read error or invalid format.
   */
  private static void readControlEvent(InputStream input,
                                       int code,
                                       OutputStream output) throws IOException {
    int length = 0;
    int type = (code & 0xF0);
    switch (type) {
      case 0x80: length = 2; break;
      case 0x90: length = 2; break;
      case 0xA0: length = 2; break;
      case 0xB0: length = 2; break;
      case 0xC0: length = 1; break;
      case 0xD0: length = 1; break;
      case 0xE0: length = 2; break;
      default: {
        throw new IOException("Invalid midi control event type " + type + ".");
      }
    }
    MidiUtil.copyBytes(input, length, output);
  }

  /**
   * Copies one midi meta event message from input to output.
   * @param input - The input stream to read from.
   * @param output - The output stream to write to.
   * @throws IOException - On any kind of read error or invalid format.
   */
  private static void readMetaEvent(InputStream input,
                                    OutputStream output) throws IOException {
    MidiUtil.copyByte(input, output);  // Copy the subtype.
    int size = MidiUtil.copyVarInt(input, output);
    MidiUtil.copyBytes(input, size, output);
  }

  /**
   * Copies one midi SysEx event message from input to output.
   * @param input - The input stream to read from.
   * @param output - The output stream to write to.
   * @throws IOException - On any kind of read error or invalid format.
   */
  private static void readSysExEvent(InputStream input,
                                     OutputStream output) throws IOException {
    int size = MidiUtil.copyVarInt(input, output);
    MidiUtil.copyBytes(input, size, output);
  }

  /**
   * Reads string.length bytes from input and verifies that they match the contents of string.
   * @param input - The input stream to read from.
   * @param string - The string to match the contents of.
   * @throws IOException if the stream doesn't have the string as its next content.
   */
  private static void verifyString(InputStream input, String string) throws IOException {
    for (int i = 0; i < string.length(); ++i) {
      int b = input.read();
      if (b < 0) {
        throw new IOException("Unexpected EOF.");
      }
      if (string.charAt(i) != (char)b) {
        throw new IOException("Invalid format. " +
                              "Expected " + string.charAt(i) + ". " +
                              "Got " + (char)b + ".");
      }
    }
  }
}
