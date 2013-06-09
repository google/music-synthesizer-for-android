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
package com.levien.synthesizer.core.wave;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A wrapper around an InputStream that provides some utility functions for reading RIFF files.
 */
public class RiffInputStream extends FilterInputStream {
  /**
   * Creates a new RiffInputStream wrapping the given input stream.
   */
  public RiffInputStream(InputStream in) {
    super(in);
  }

  /**
   * Skips the specified number of bytes in the input.
   */
  public void skipBytes(long bytes) throws IOException {
    while (bytes > 0) {
      int skipped = (int)skip(bytes);
      if (skipped <= 0) {
        throw new IOException("Error skipping bytes.");
      }
      bytes -= skipped;
    }
  }

  /**
   * Reads one signed byte from an input stream.
   * @param output - the stream to read from.
   * @return the signed byte read.
   * @throws IOException - on any kind of read error or EOF.
   */
  public byte readChar() throws IOException {
    return (byte)readByte();
  }

  /**
   * Reads one unsigned byte from an input stream.
   * @param output - the stream to read from.
   * @return the unsigned byte read.
   * @throws IOException - on any kind of read error or EOF.
   */
  public short readByte() throws IOException {
    int b = read();
    if (b < 0) {
      throw new IOException("Unexpected EOF while reading byte.");
    }
    return (short)(b & 0xFF);
  }

  /**
   * Checks that the given String matches the next bytes in the input stream.
   * @param input - the stream to read from.
   * @param data - the String to check for.
   * @throws IOException - On any kind of read error or if the exact string wasn't found.
   */
  public void checkBytes(String data) throws IOException {
    byte[] bytes = data.getBytes();
    for (int i = 0; i < bytes.length; ++i) {
      int b = read();
      if (b < 0) {
        throw new IOException("Expected " + bytes[i] + ", got EOF, while looking for " + data);
      }
      if ((byte)b != bytes[i]) {
        throw new IOException("Expected " + bytes[i] + ", got " + (byte)b +
                              ", while looking for " + data);
      }
    }
  }

  /**
   * Reads the given number of bytes and returns an array of them.
   * @param input - the stream to read from.
   * @param size - the number of bytes to read.
   * @throws IOException - On any kind of read error or EOF.
   */
  public byte[] readBytes(int size) throws IOException {
    // TODO(klimt):  This could be more efficient.
    byte[] bytes = new byte[size];
    int length = 0;
    for (length = 0; length < bytes.length; ++length) {
      int b = read();
      if (b < 0) {
        throw new IOException("Unexpected EOF while reading bytes.");
      }
      bytes[length] = (byte)b;
    }
    return bytes;
  }

  /**
   * Reads a string of the given size from the input stream.
   * I'm not sure what happens if it's not ASCII.
   * @param input - the stream to read from.
   * @param size - the number of bytes to read.
   * @throws IOException - On any kind of read error or if the exact string wasn't found.
   */
  public String readString(int size) throws IOException {
    byte[] bytes = new byte[size];
    int length = 0;
    for (length = 0; length < bytes.length; ++length) {
      int b = read();
      if (b < 0) {
        throw new IOException("Unexpected EOF while reading string.");
      }
      if (b == 0) {
        skipBytes(size - (length + 1));
        break;
      }
      bytes[length] = (byte)b;
    }
    String s = new String(bytes, 0, length);
    if (s.length() > size) {
      throw new IOException("Read string \"" + s + "\" longer than " + size + " bytes.");
    }
    return s;
  }

  /**
   * Reads one unsigned dword (32 bits) from an input stream in little-endian format.
   * @param input - the stream to read from.
   * @return - the unsigned dword read.
   * @throws IOException - on any kind of read error or EOF.
   */
  public long readDWord() throws IOException {
    long byte1 = read();
    long byte2 = read();
    long byte3 = read();
    long byte4 = read();
    if (byte1 < 0 || byte2 < 0 || byte3 < 0 || byte4 < 0) {
      throw new IOException("Unexpected EOF while reading int32.");
    }
    return (byte4 << 24) | (byte3 << 16) | (byte2 << 8) | byte1;
  }

  /**
   * Reads one signed word (16 bits) from an input stream in little-endian format.
   * @param output - the stream to read from.
   * @return the signed word read.
   * @throws IOException - on any kind of read error or EOF.
   */
  public short readShort() throws IOException {
    return (short)readWord();
  }

  /**
   * Reads one unsigned word (16 bits) from an input stream in little-endian format.
   * @param output - the stream to read from.
   * @return the unsigned word read.
   * @throws IOException - on any kind of read error or EOF.
   */
  public int readWord() throws IOException {
    int lower = read();
    int upper = read();
    if (lower < 0 || upper < 0) {
      throw new IOException("Unexpected EOF while reading int16.");
    }
    int value = (int)(((upper << 8) | lower) & 0xFFFF);
    return value;
  }
}
