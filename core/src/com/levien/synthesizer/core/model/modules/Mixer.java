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
 * Mixer outputs a weighted average of two input signals.
 * A "balance" of 0.0 outputs signal 1, and a balance of 1.0 outputs signal 2.
 * The balance can be changed over time.
 */
public class Mixer implements SignalProvider {
  /**
   * Creates a new Mixer.
   * @param signal1 - Any input signal module.
   * @param signal2 - Any input signal module.
   * @param balance - A module outputting the weight to use when averaging.
   */
  public Mixer(SignalProvider signal1, SignalProvider signal2, SignalProvider balance) {
    signal1_ = signal1;
    signal2_ = signal2;
    balance_ = balance;
  }

  /**
   * Returns the average of the input signals, weighted by balance.
   */
  public double getValue(SynthesisTime time) {
    double balance = balance_.getValue(time);
    // As an optimization, don't compute any signal that's not needed.
    if (balance == 0.0) {
      return signal1_.getValue(time);
    } else if (balance == 1.0) {
      return signal2_.getValue(time);
    } else {
      return (1.0 - balance) * signal1_.getValue(time) + balance * signal2_.getValue(time);
    }
  }

  private SignalProvider signal1_;
  private SignalProvider signal2_;
  private SignalProvider balance_;
}
