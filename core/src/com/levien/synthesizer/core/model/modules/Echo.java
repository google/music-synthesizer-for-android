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

import com.levien.synthesizer.core.model.CachedSignalProvider;
import com.levien.synthesizer.core.model.SignalProvider;
import com.levien.synthesizer.core.model.SynthesisTime;

/**
 * The Echo module produces an echo effect by mixing its current input with its earlier input.
 */
public class Echo extends CachedSignalProvider {
  /**
   * Creates a new Echo module.
   * @param source - The input to the echo module.
   * @param mix - How wet/dry the output is, i.e. the depth of the effect.
   * @param delay - The length of time in seconds between input and its first repetition.
   * @param sampleRateInHz - Sample rate, used to compute buffer size from delay time.
   */
  public Echo(SignalProvider source,
              SignalProvider mix,
              SignalProvider delay,
              double sampleRateInHz) {
    source_ = source;
    mix_ = mix;
    delay_ = delay;
    sampleRateInHz_ = sampleRateInHz;
    buffer_ = new double[MAX_BUFFER_SIZE];
    for (int i = 0; i < MAX_BUFFER_SIZE; ++i) {
      buffer_[i] = 0.0;
    }
    bufferSize_ = 0;
    current_ = 0;
    previousDelay_ = 0.0;
  }

  protected void maybeUpdateDelay(double delay) {
    // As an optimization, just bail if the delay value hasn't changed.
    if (previousDelay_ == delay) {
      return;
    }
    previousDelay_ = delay;

    int newBufferSize = 0;
    if (delay != 0.0) {
      newBufferSize = (int)Math.round(delay * sampleRateInHz_);
    }
    if (bufferSize_ == newBufferSize) {
      return;
    }
    bufferSize_ = newBufferSize;
    if (bufferSize_ < 1) {
      bufferSize_ = 0;
    }
    if (bufferSize_ > MAX_BUFFER_SIZE) {
      bufferSize_ = MAX_BUFFER_SIZE;
    }
  }

  @Override
  protected double computeValue(SynthesisTime time) {
    maybeUpdateDelay(delay_.getValue(time));
    double input = source_.getValue(time);
    double mix = mix_.getValue(time);
    if (bufferSize_ == 0) {
      return (1.0 - mix) * input;
    } else {
      double value = mix * buffer_[current_] + (1.0 - mix) * input;
      buffer_[current_] = value;
      current_ = (current_ + 1) % bufferSize_;
      return value;
    }
  }

  private SignalProvider source_;
  private SignalProvider mix_;
  private SignalProvider delay_;

  private double previousDelay_;
  private double[] buffer_;
  private int bufferSize_;
  private double sampleRateInHz_;
  private int current_;

  private final static int MAX_BUFFER_SIZE = 55125;
}
