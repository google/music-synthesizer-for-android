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
 * An oscillator module that outputs a triangle wave.
 */
public class Triangle extends Oscillator {
  public Triangle(FrequencyProvider frequency) {
    super(frequency);
    sawtooth_ = new Sawtooth(frequency);
  }

  public double computeValue(SynthesisTime time) {
    // It's a simple mathematical transformation to convert a sawtooth to a triangle.
    return -2.0 * Math.abs(sawtooth_.getValue(time)) + 1.0;
  }

  private Sawtooth sawtooth_;
}
