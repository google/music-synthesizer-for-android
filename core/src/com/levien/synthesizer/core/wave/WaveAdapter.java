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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.levien.synthesizer.core.model.CachedSignalProvider;
import com.levien.synthesizer.core.model.SignalProvider;
import com.levien.synthesizer.core.model.SynthesisTime;

/**
 * A WaveAdapter wraps a synthesizer component and allows siphoning off its output and writing it to
 * a wave file.
 * @see WaveWriter
 */
public class WaveAdapter extends CachedSignalProvider {
  /**
   * Creates a new WaveAdapter.
   * @param sampleRate - The sample rate of the synthesizer in Hz.
   * @param bitsPerSample - The bits per sample of the wave file to write.  Should be 8 or 16.
   * @param source - The synthesizer component to siphon output from.
   */
  public WaveAdapter(int sampleRate, int bitsPerSample, SignalProvider source) {
    logger_ = Logger.getLogger(getClass().getName());
    sampleRate_ = sampleRate;
    bitsPerSample_ = bitsPerSample;
    source_ = source;
  }

  /**
   * Starts the WaveAdapter recording for a specified length of time.
   * @param seconds - The length of time to record.
   * @parma output - The stream to write the output to.
   */
  public synchronized void startRecording(double seconds, OutputStream output) {
    if (writer_ != null) {
      try {
        writer_.close();
      } catch (IOException e) {
        logger_.log(Level.SEVERE, "Unable to close wave file.", e);
      }
      writer_ = null;
    }
    writer_ = new WaveWriter(sampleRate_, bitsPerSample_);
    logger_.info("Recording " + seconds + " seconds to file.");
    try {
      writer_.startRecording(seconds, output);
    } catch (IOException e) {
      logger_.log(Level.SEVERE, "Unable to open wave file for writing.", e);
    }
  }

  /**
   * Reads one sample from source, writes it to output, and returns it.
   */
  @Override
  protected synchronized double computeValue(SynthesisTime time) {
    double value = source_.getValue(time);
    if (writer_ != null) {
      try {
        if (!writer_.writeSample(value)) {
          // The file is full.
          logger_.info("Finished writing wave file.");
          close();
        }
      } catch (IOException e) {
        logger_.log(Level.SEVERE, "Unable to write sample to wave file.  Aborting.", e);
        close();
      }
    }
    return value;
  }

  /**
   * Makes sure the file is closed.  You don't need to call this between recordings, but only when
   * you are done, to make sure the last wave file was closed.  If the file has already finished
   * recording, then this method will not do anything.
   */
  public synchronized void close() {
    if (writer_ != null) {
      try {
        writer_.close();
      } catch (IOException e) {
        logger_.log(Level.SEVERE, "Unable to close the wave file.  Aborting.", e);
      }
      writer_ = null;
    }
  }

  // The sample rate of the source and output, in Hz.
  private int sampleRate_;

  // The number of bits per sample specified when creating the file.  Should be 8 or 16.
  private int bitsPerSample_;

  // The synthesizer module to siphon output from.
  private SignalProvider source_;

  // The underlying writer that does the real work of creating the wave file.
  private WaveWriter writer_;

  private Logger logger_;
}
