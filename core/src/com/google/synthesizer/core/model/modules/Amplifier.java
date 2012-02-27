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

import com.google.synthesizer.core.model.SignalProvider;
import com.google.synthesizer.core.model.SynthesisTime;

/**
 * Amplifier simply multiplies the input signal by the input gain.
 */
public class Amplifier implements SignalProvider {
  /**
   * Creates a new Amplifier with the given signal and gain.
   * @param inputSignal - Any input signal.
   * @param gain - The signal it's multiplied times.
   */
  public Amplifier(SignalProvider inputSignal, SignalProvider gain) {
    inputSignal_ = inputSignal;
    gain_ = gain;
  }

  /**
   * Returns signal * gain.
   */
  public double getValue(SynthesisTime time) {
    // Most of the time, the gain will be 0.0 because of some envelope.  So this is an optimization.
    double gain = gain_.getValue(time);
    if (gain == 0.0) {
      return 0.0;
    }
    return inputSignal_.getValue(time) * gain;
  }

  private SignalProvider inputSignal_;
  private SignalProvider gain_;
}
