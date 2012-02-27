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

package com.google.synthesizer.core.model.oscillator;

import com.google.synthesizer.core.model.FrequencyProvider;
import com.google.synthesizer.core.model.SynthesisTime;

/**
 * An oscillator module that outputs a sine wave.
 */
public class Sine extends Oscillator {
  public Sine(FrequencyProvider frequency) {
    super(frequency);
    value_ = 0.0;
    offset_ = 0.0;
    currentLogFrequency_ = 0.0;
    currentFrequency_ = 1.0;
  }
  
  public double computeValue(SynthesisTime time) {
    // Compute the output using the current frequency.
    value_ = Math.sin(2 * Math.PI * currentFrequency_ * (time.getAbsoluteTime() + offset_));

    // If the frequency is supposed to change, change the phase offset to where the new frequency
    // would be outputting the same current value.
    double nextLogFrequency = frequency_.getLogFrequency(time);
    if (currentLogFrequency_ != nextLogFrequency) {
      double nextFrequency = Math.pow(2.0, nextLogFrequency);
      offset_ = ((time.getAbsoluteTime() + offset_) * currentFrequency_ / nextFrequency) -
                  time.getAbsoluteTime();
      currentLogFrequency_ = nextLogFrequency;
      currentFrequency_ = nextFrequency;
    }

    return value_;
  }

  // Most recently output value.
  private double value_;

  // How far out of phrase the wave is from one starting at time 0.
  private double offset_;

  // Amplitude and frequency for the current output value.
  private double currentLogFrequency_;
  private double currentFrequency_;
}