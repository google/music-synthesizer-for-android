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
import java.io.OutputStream;

/**
 * A collection of basic functions for reading Midi data from a stream.
 */
public class MidiUtil {
  /**
   * Copies a sequence of bytes from one stream to another.
   * @param input - The stream to read from.
   * @param size - The number of bytes to copy.
   * @param output - The stream to write to.
   * @throws IOException on EOF or any read or write error.
   */
  public static void copyBytes(InputStream input,
                               int size,
                               OutputStream output) throws IOException {
    if (size == 0) {
      return;
    }
    byte[] buffer = new byte[size];
    readBytes(input, size, buffer);
    output.write(buffer);
  }

  /**
   * Reads a sequency of bytes into a byte array.
   * @param input - The stream to read from.
   * @param size - The number of bytes to read.
   * @param buffer - The array to store the bytes into.
   * @throws IOException on EOF or any read error.
   */
  public static void readBytes(InputStream input, int size, byte[] buffer) throws IOException {
    // Read "size" bytes into an array.
    int totalRead = input.read(buffer, 0, size);
    while (totalRead < size) {
      int read = input.read(buffer, totalRead, size - totalRead);
      if (read < 0) {
        throw new IOException("Unexpected EOF while reading bytes.");
      }
      totalRead += read;
    }
  }

  /**
   * Reads a variable-size int from input, as defined by the Midi format.
   * @param input - The stream to read from.
   * @throws IOException on EOF or any read error.
   */
  public static int readVarInt(InputStream input) throws IOException {
    return copyVarInt(input, null);
  }

  /**
   * Writes a variable-size int to output, as defined by the Midi format.
   * @param output - The stream to write to.
   * @throws IOException on any write error.
   */
  public static void writeVarInt(OutputStream output, int value) throws IOException {
    // The bits are laid out like this:
    // 11112222 22233333 33444444 45555555
    //
    // It's easier to just enumerate them than to make a loop.
    //
    if ((value & 0xF0000000) != 0) {
      output.write(0x80 | ((value >> 28) & 0x7F));
    }
    if ((value & 0xFFE00000) != 0) {
      output.write(0x80 | ((value >> 21) & 0x7F));
    }
    if ((value & 0xFFFFC000) != 0) {
      output.write(0x80 | ((value >> 14) & 0x7F));
    }
    if ((value & 0xFFFFFF80) != 0) {
      output.write(0x80 | ((value >> 7) & 0x7F));
    }
    output.write(value & 0x7F);
  }

  /**
   * Copies a variable-size int from input to output, as defined by the Midi format.
   * @param input - The stream to read from.
   * @param output - The stream to write to.
   * @throws IOException on EOF or any read or write error.
   */
  public static int copyVarInt(InputStream input, OutputStream output) throws IOException {
    long value = 0;
    int b = copyByte(input, output);
    value = b & 0x7F;
    while ((b & 0x80) != 0) {
      b = copyByte(input, output);
      value = (value << 7) | (b & 0x7F);
    }
    return (int)value;
  }

  /**
   * Reads a 32-bit signed value from input.
   * @throws IOException on EOF or any read error.
   */
  public static int readDWord(InputStream input) throws IOException {
    long w1 = readWord(input);
    long w2 = readWord(input);
    return (int)(((w1 & 0xFFFF) << 16) | (w2 & 0xFFFF));
  }

  /**
   * Reads a 16 bit unsigned value from input.
   * @throws IOException on EOF or any read error.
   */
  public static int readWord(InputStream input) throws IOException {
    int b1 = readByte(input);
    int b2 = readByte(input);
    return ((b1 & 0xFF) << 8) | (b2 & 0xFF);
  }

  /**
   * Writes a 16 bit unsigned value to output.
   * @param output - The stream to write to.
   * @param value - The value to write out.
   * @throws IOException on any write error.
   */
  public static void writeWord(OutputStream output, int value) throws IOException {
    int b1 = (value >> 8) & 0xFF;
    int b2 = value & 0xFF;
    output.write(b1);
    output.write(b2);
  }

  /**
   * Reads an 8-bit unsigned value from input.
   * @throws IOException on EOF or any read error.
   */
  public static int readByte(InputStream input) throws IOException {
    return copyByte(input, null);
  }

  /**
   * Copies one byte from input to output.
   * @param input - The stream to read from.
   * @param output - The stream to write to.
   * @throws IOException on EOF or any read or write error.
   */
  public static int copyByte(InputStream input, OutputStream output) throws IOException {
    int b = input.read();
    if (b < 0) {
      throw new IOException("Unexpected EOF.");
    }
    if (output != null) {
      output.write(b);
    }
    return b & 0xFF;
  }

}
