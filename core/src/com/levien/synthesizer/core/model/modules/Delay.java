/*
 * Copyright 2010 Google Inc.
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

package com.levien.synthesizer.core.model.modules;

import java.util.Arrays;

import com.levien.synthesizer.core.model.CachedSignalProvider;
import com.levien.synthesizer.core.model.SignalProvider;
import com.levien.synthesizer.core.model.SynthesisTime;

/**
 * Delay is a module for recording sampled sound and playing it back later.  It has 3 modes.
 * In regular mode, it outputs its input, attenuated by half.
 * In record mode, it also captures sound until it is stopped or the MAX_BUFFER_SIZE is reached.
 * In play mode, it plays back its output mixed with the recorded buffer in a loop.
 */
public class Delay extends CachedSignalProvider {
  public Delay(SignalProvider source, SignalProvider mix) {
    source_ = source;
    mix_ = mix;
    buffer_ = new double[MAX_BUFFER_SIZE];
    current_ = 0;
    bufferSize_ = MAX_BUFFER_SIZE;
    Arrays.fill(buffer_, 0.0);
    playing_ = false;
    recording_ = false;
  }

  @Override
  protected synchronized double computeValue(SynthesisTime time) {
    double input = source_.getValue(time);
    double mix = mix_.getValue(time);
    if (playing_) {
      double output = mix * buffer_[current_] + (1.0 - mix) * input;
      current_ = (current_ + 1) % bufferSize_;
      return output;
    } else {
      if (recording_) {
        buffer_[bufferSize_++] = input;
        if (bufferSize_ >= MAX_BUFFER_SIZE) {
          stopRecording();
        }
      }
      return (1.0 - mix) * input;
    }
  }

  /**
   * Resets the buffer and changes to record mode.
   */
  public synchronized void startRecording() {
    playing_ = false;
    recording_ = true;
    current_ = 0;
    bufferSize_ = 0;
  }

  /**
   * Stops recording, but doesn't start playing.
   */
  public synchronized void stopRecording() {
    recording_ = false;
    current_ = 0;
  }

  /**
   * Starts playing what's recorded.
   */
  public synchronized void startPlaying() {
    playing_ = true;
    recording_ = false;
    current_ = 0;
  }

  /**
   * Stops playing and recording.
   */
  public synchronized void stopPlaying() {
    playing_ = false;
    recording_ = false;
    current_ = 0;
  }

  /**
   * Returns whether the module is in playing mode.
   */
  public synchronized boolean isPlaying() {
    return playing_;
  }

  /**
   * Returns whether the module is in record mode.
   */
  public synchronized boolean isRecording() {
    return recording_;
  }

  private SignalProvider source_;
  private SignalProvider mix_;

  private double[] buffer_;
  private int bufferSize_;
  private int current_;

  private boolean playing_;
  private boolean recording_;
  
  private final static int MAX_BUFFER_SIZE = 10;
}
