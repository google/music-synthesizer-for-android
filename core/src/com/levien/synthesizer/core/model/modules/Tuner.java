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

import com.levien.synthesizer.core.model.FrequencyProvider;
import com.levien.synthesizer.core.model.SignalProvider;
import com.levien.synthesizer.core.model.SynthesisTime;

/**
 * Module to modify the output of a frequency provider.
 */
public class Tuner implements FrequencyProvider {
  /**
   * Creates a Tuner module to wrap source.
   * @param input - The base log frequency.
   * @param shift - The amount to alter it.
   */
  public Tuner(FrequencyProvider input, SignalProvider shift) {
    input_ = input;
    shift_ = shift;
  }
  
  public double getLogFrequency(SynthesisTime time) {
    double input = input_.getLogFrequency(time);
    double shift = shift_.getValue(time);    
    return input + shift;
  }

  // The wrapped frequency provider to take as input.
  private FrequencyProvider input_;

  // The amount to alter the signal.
  private SignalProvider shift_;
}
