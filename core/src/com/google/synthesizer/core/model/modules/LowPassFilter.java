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

package com.google.synthesizer.core.model.modules;

import com.google.synthesizer.core.model.CachedSignalProvider;
import com.google.synthesizer.core.model.SignalProvider;
import com.google.synthesizer.core.model.SynthesisTime;

/**
 * A very simple low-pass filter.
 */
public class LowPassFilter extends CachedSignalProvider {
  /**
   * Creates a LowPassFilter with the given parameters.
   * @param source - The input signal to filter.
   * @param cutoff - "Alpha" parameter that controls the cutoff frequency.
   */
  public LowPassFilter(SignalProvider source, SignalProvider cutoff) {
    source_ = source;
    alpha_ = cutoff;
    previousValue_ = 0.0;
  }

  public double computeValue(SynthesisTime time) {
    double alpha = alpha_.getValue(time);
    double output = 0.0;
    if (alpha != 0) {
      double signal = source_.getValue(time);
      output = previousValue_ + alpha * (signal - previousValue_);
      previousValue_ = output;
    } else {
      output = previousValue_;
    }
    return output;
  }

  // The filter parameters.
  private SignalProvider source_;
  private SignalProvider alpha_;

  private double previousValue_;
}
