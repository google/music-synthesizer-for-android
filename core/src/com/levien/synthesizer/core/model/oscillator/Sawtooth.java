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

package com.levien.synthesizer.core.model.oscillator;

import com.levien.synthesizer.core.model.FrequencyProvider;
import com.levien.synthesizer.core.model.SynthesisTime;

/**
 * An oscillator module that outputs a sawtooth wave.
 */
public class Sawtooth extends Oscillator {
  public Sawtooth(FrequencyProvider frequency) {
    super(frequency);
    value_ = 0.0;
    currentFrequency_ = 1.0;
  }
  
  public double computeValue(SynthesisTime time) {
    // The output value drops until it is below -1.0, then shoots up to 1.0.
    value_ -= (2.0 * currentFrequency_ * time.getDeltaTime());
    while (value_ <= -1.0) {
      // Frequency is only updated at the end of each cycle.
      double nextFrequency = Math.pow(2.0, frequency_.getLogFrequency(time));
      currentFrequency_ = nextFrequency;
      // We can't just set to 1.0.  The signal should have decreased from 1.0 by the amount it
      // actually decreased below -1.0.
      value_ += 2.0;
    }
    return value_;
  }

  // The most recent output value.
  private double value_;

  // The current frequency of the waveform.
  private double currentFrequency_;
}
