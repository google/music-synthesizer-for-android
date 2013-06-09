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

import com.levien.synthesizer.core.model.SignalProvider;
import com.levien.synthesizer.core.model.SynthesisTime;

/**
 * Module to modulate an amplitude over time based on a modulator signal.  Its output is intended to
 * be fed into an amplifier.
 */
public class Tremolo implements SignalProvider {
  /**
   * Creates a Tremolo module.
   * @param modulator - Waveform that modulates the amplitude.
   * @param depth - Depth of amplitude modulation.
   */
  public Tremolo(SignalProvider modulator, SignalProvider depth) {
    modulator_ = modulator;
    depth_ = depth;
  }
  
  public double getValue(SynthesisTime time) {
    double modulator = modulator_.getValue(time);
    double depth = depth_.getValue(time);
    return (modulator * (depth / 2.0)) + (1.0 - depth / 2.0);
  }

  // Waveform that modulates the amplitude.
  private SignalProvider modulator_;

  // Depth of amplitude modulation.
  private SignalProvider depth_;
}
