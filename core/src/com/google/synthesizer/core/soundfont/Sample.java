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

package com.google.synthesizer.core.soundfont;

import java.io.IOException;

import com.google.synthesizer.core.wave.RiffInputStream;

/**
 * A Sample is a subset of sample data from a SoundFont file.
 */
public class Sample {
  /**
   * Reads one SampleHeader from the given input stream.
   * @param input - A SoundFont input stream.
   * @param samples - All of the sample data from the SoundFont file.
   */
  public Sample(RiffInputStream input, double[] samples) throws IOException {
    name_ = input.readString(20);
    start_ = (int)input.readDWord();
    end_ = (int)input.readDWord();
    startLoop_ = (int)input.readDWord();
    endLoop_ = (int)input.readDWord();
    sampleRate_ = input.readDWord();
    originalKey_ = input.readByte();
    correction_ = input.readChar();
    sampleLink_ = input.readWord();
    sampleType_ = SampleLink.fromType(input.readWord());
    if (sampleType_.equals(SampleLink.UNKNOWN) &&
        !name_.equals("EOS")) {
      throw new IOException("Unsupported sample type.");
    }
    sample_ = samples;
  }

  /**
   * @return The index of the first sample in the sample.
   */
  public long getStart() {
    return start_;
  }

  /**
   * @return The index of one past the last sample in the sample.
   */
  public long getEnd() {
    return end_;
  }

  /**
   * @return The sample rate of the sample.
   */
  public long getRate() {
    return sampleRate_;
  }

  /**
   * @return The number of samples in the sample.
   */
  public int getCount() {
    return end_ - start_;
  }

  /**
   * Returns one sample from the, um, sample.
   * @param i - The index of the sample.  Should be between getStart() and getEnd().
   */
  public double getSample(int i) {
    return sample_[i];
  }

  /**
   * Returns a human-readable description of the sample.
   */
  public String toString() {
    return
        "SampleHeader {\n" +
        "  name: " + name_ + "\n" +
        "  start: " + start_ + "\n" +
        "  end: " + end_ + "\n" +
        "  start loop: " + startLoop_ + "\n" +
        "  end loop: " + endLoop_ + "\n" +
        "  sample rate: " + sampleRate_ + "\n" +
        "  original key: " + originalKey_ + "\n" +
        "  correction: " + correction_ + "\n" +
        "  sample link: " + sampleLink_ + "\n" +
        "  sample type: " + sampleType_.toString() + " (" + sampleType_.getType() + ")\n" +
        "}";
  }

  // The info from the SampleHeader.
  private String name_;
  private int start_;
  private int end_;
  private int startLoop_;
  private int endLoop_;
  private long sampleRate_;
  private short originalKey_;
  private byte correction_;
  private int sampleLink_;
  private SampleLink sampleType_;

  // All of the actual sample data from the SoundFont file.
  private double[] sample_;
}