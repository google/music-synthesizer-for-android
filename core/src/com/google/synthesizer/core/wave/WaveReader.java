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

package com.google.synthesizer.core.wave;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

/**
 * Reads a WAVE file from a stream and provides access its data.
 */
public class WaveReader {
  /**
   * Reads the WAVE file from a stream and stores the data in a buffer.
   * @param input - the stream to read from.
   * @throws IOExpection - on a malformed or unsupported file format.
   */
  public WaveReader(InputStream in) throws IOException {
    Logger logger = Logger.getLogger(getClass().getName());
    logger.info("Loading WAVE file.");

    RiffInputStream input = new RiffInputStream(in);

    // Header.
    input.checkBytes("RIFF");
    logger.info("Read RIFF");
    @SuppressWarnings("unused")
    int chunkSize = (int)input.readDWord();
    input.checkBytes("WAVE");

    // Sub-chunk 1.
    input.checkBytes("fmt ");
    int subChunk1Size = (int)input.readDWord();
    if (subChunk1Size != 16) {
      throw new IOException("Format is not PCM.");
    }
    int audioFormat = input.readWord();
    if (audioFormat != 1) {
      throw new IOException("Unsupported compression scheme: " + audioFormat);
    }
    numChannels_ = input.readWord();
    if (numChannels_ != 1 && numChannels_ != 2) {
      throw new IOException("Unsupported number of channels: " + numChannels_);
    }
    sampleRateInHz_ = (int)input.readDWord();
    @SuppressWarnings("unused")
    int byteRate = (int)input.readDWord();
    @SuppressWarnings("unused")
    int blockAlign = input.readWord();
    int bitsPerSample = input.readWord();
    if (bitsPerSample != 8 && bitsPerSample != 16) {
      throw new IOException("Unsupported bits per sample: " + bitsPerSample);
    }
    int bytesPerSample = bitsPerSample / 8;

    // Sub-chunk 2.
    input.checkBytes("data");
    int subChunk2Size = (int)input.readDWord();

    // Actual data.
    int numSamples = subChunk2Size / (numChannels_ * bytesPerSample);
    leftChannel_ = new double[numSamples];
    rightChannel_ = new double[numSamples];
    for (int i = 0; i < numSamples; ++i) {
      switch (bitsPerSample) {
        case 8:
          leftChannel_[i] = input.readChar() / 128.0;
          break;
        case 16:
          leftChannel_[i] = input.readShort() / 32768.0;
          break;
      }
      if (numChannels_ == 2) {
        switch (bitsPerSample) {
          case 8:
            rightChannel_[i] = input.readChar() / 128.0;
            break;
          case 16:
            rightChannel_[i] = input.readShort() / 32768.0;
            break;
        }
      } else {
        rightChannel_[i] = leftChannel_[i];
      }
    }

    input.close();
  }

  /**
   * Returns the sample rate of the wave file.
   */
  public int getSampleRateInHz() {
    return sampleRateInHz_;
  }

  /**
   * Returns the number of samples in the wave file.
   */
  public int getSize() {
    return leftChannel_.length;
  }

  /**
   * Returns the nth sample from the left channel of the wave file.
   * If it's a mono file, returns the one sample.
   */
  public double getLeftSample(int i) {
    return leftChannel_[i];
  }

  /**
   * Returns the nth sample from the right channel of the wave file.
   * If it's a mono file, returns the one sample.
   */
  public double getRightSample(int i) {
    return rightChannel_[i];
  }

  /**
   * Returns the nth sample from the  wave file.
   * If it's a mono file, returns the one sample.  It it's stereo, returns the average.
   */
  public double getMonoSample(int i) {
    return (leftChannel_[i] + rightChannel_[i]) / 2;
  }

  // The number of channels in the file.
  private int numChannels_;

  // The sample rate of the file.
  private int sampleRateInHz_;

  // The samples in the file.
  private double[] leftChannel_;
  private double[] rightChannel_;
}
