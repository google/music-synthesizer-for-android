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
 * An oscillator module that outputs a square wave.
 */
public class Square extends Oscillator {
  public Square(FrequencyProvider frequency) {
    super(frequency);
    sine_ = new Sine(frequency);
  }

  public double computeValue(SynthesisTime time) {
    // The easiest way to make a square wave is to take a sine wave and round it to -1 or 1.
    if (sine_.getValue(time) > 0.0) {
      return 1.0;
    } else {
      return -1.0;
    }
  }

  private Sine sine_;
}
