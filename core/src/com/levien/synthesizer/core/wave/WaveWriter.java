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

import java.io.IOException;
import java.io.OutputStream;

/**
 * WaveWriter is a class for writing sampled audio data to an output stream as a wave file.
 * To use it, call startRecording.  Then call writeSample until it returns false.  Then call close.
 * @see WaveAdapter
 */
public class WaveWriter {
  /**
   * Creates a new WaveWriter with the given parameters.
   * @param sampleRate - The sample rate in Hz.
   * @param bitsPerSample - The size of each sample.  Should be 8 or 16.
   *     8-bit is assumed to be unsigned.  16-bit is assumed to be signed.
   */
  public WaveWriter(int sampleRate, int bitsPerSample) {
    sampleRate_ = sampleRate;
    bitsPerSample_ = bitsPerSample;
    if (bitsPerSample_ != 8 && bitsPerSample_ != 16) {
      throw new RuntimeException(
          "Unacceptable bits per sample: " + bitsPerSample_ + ". Try 8 or 16.");
    }
    samples_ = 0;
    output_ = null;
  }

  /**
   * Initializes the wave file and writes its header.
   * @param seconds - How many seconds of recording to do.
   * @param output - The stream to write the wave file to.
   * @throws IOException - On any kind of write error.
   */
  public void startRecording(double seconds, OutputStream output) throws IOException {
    if (output_ != null) {
      close();
    }

    samples_ = (int)Math.round(sampleRate_ * seconds);
    try {
      output_ = output;
  
      writeBytes("RIFF", output_);
      writeLittle32(chunkSize(samples_), output_);
      writeBytes("WAVE", output_);
  
      // Sub-chunk 1
      writeBytes("fmt ", output_);
      writeLittle32(16, output_);
      writeLittle16(1, output_);
      writeLittle16(numChannels(), output_);
      writeLittle32(sampleRate_, output_);
      writeLittle32(byteRate(), output_);
      writeLittle16(blockAlign(), output_);
      writeLittle16(bitsPerSample(), output_);
  
      // Sub-chunk 2
      writeBytes("data", output_);
      writeLittle32(subChunk2Size(samples_), output_);
    } catch (IOException e) {
      if (output_ != null) {
        output_.close();
      }
      output_ = null;
      samples_ = 0;
      throw e;
    }
  }

  /**
   * Writes one sample of audio data to the file.  Closes the stream when the file is full.
   * @param sample - The value to output, expected to be in the range [-1, 1].
   * @return False when the sample can't be written because the file is full.  True otherwise.
   * @throws IOException - On any kind of write error.
   */
  public boolean writeSample(double sample) throws IOException {
    if (samples_ == 0) {
      return false;
    }
    // Clamp values out of range.
    if (sample < -1.0) {
      sample = -1.0;
    }
    if (sample > 1.0) {
      sample = 1.0;
    }
    try {
      if (bitsPerSample() == 16) {
        short shortSample = (short)(32767 * sample);
        writeLittle16(shortSample, output_);
      } else if (bitsPerSample() == 8) {
        writeByte((int)(255 * sample + 127.5), output_);
      }
      --samples_;
      if (samples_ == 0) {
        output_.close();
      }
    } catch (IOException e) {
      if (output_ != null) {
        output_.close();
        output_ = null;
      }
      samples_ = 0;
      throw e;
    }
    return true;
  }

  /**
   * Pads the file until it is the pre-specified length, and then closes the stream.
   */
  public void close() throws IOException {
    while (samples_ > 0) {
      writeSample(0.0);
    }
    output_.close();
  }

  /**
   * Returns the chunk size for the wave header.
   */
  private int chunkSize(int samples) {
    return 36 + subChunk2Size(samples);
  }

  /**
   * Returns the second sub-chunk size for the wave header.
   */
  private int subChunk2Size(int samples) {
    return samples * numChannels() * bitsPerSample() / 8;
  }

  /**
   * Returns the number of channels for the wave file.
   */
  private int numChannels() {
    return 1;
  }

  /**
   * Returns the bytes per second of the wave file.
   */
  private int byteRate() {
    return sampleRate_ * numChannels() * bitsPerSample() / 8;
  }

  /**
   * Returns the block-align parameter for the wave header.
   */
  private int blockAlign() {
    return numChannels() * bitsPerSample() / 8;
  }

  /**
   * Returns the bits per sample used when creating the WaveWriter.
   */
  private int bitsPerSample() {
    return bitsPerSample_;
  }

  /**
   * Writes one byte to an output stream.
   * @param data - The unsigned byte to write.
   * @param output - The stream to write to.
   * @throws IOException - On any kind of write error.
   */
  private static void writeByte(int data, OutputStream output) throws IOException {
    output.write(data);
  }

  /**
   * Writes a String to an output stream as bytes.
   * @param data - The bytes to write.
   * @param output - The stream to write to.
   * @throws IOException - On any kind of write error.
   */
  private static void writeBytes(String data, OutputStream output) throws IOException {
    output.write(data.getBytes());
  }

   /**
   * Writes one dword (32 bits) to an output stream in little-endian format.
   * @param data - The dword to write.
   * @param output - The stream to write to.
   * @throws IOException - On any kind of write error.
    */
  private static void writeLittle32(int data, OutputStream output) throws IOException {
    output.write(data & 0xFF);
    output.write((data >> 8) & 0xFF);
    output.write((data >> 16) & 0xFF);
    output.write((data >> 24) & 0xFF);
  }

  /**
   * Writes one word (16 bits) to an output stream in little-endian format.
   * @param data - The word to write.
   * @param output - The stream to write to.
   * @throws IOException - On any kind of write error.
   */
  private static void writeLittle16(int data, OutputStream output) throws IOException {
    output.write(data & 0xFF);
    output.write((data >> 8) & 0xFF);
  }

  // The number of samples left to write until the file is full.
  private int samples_;
  // The sample rate in Hz.
  private int sampleRate_;
  // The number of bits per output sample.  Should be 8 or 16.
  private int bitsPerSample_;
  // The output stream to write the wave file to.
  private OutputStream output_;
}
