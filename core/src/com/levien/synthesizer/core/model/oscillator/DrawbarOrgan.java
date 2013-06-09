/*
 * Copyright 2011 Google Inc.
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
import com.levien.synthesizer.core.model.SignalProvider;
import com.levien.synthesizer.core.model.SynthesisTime;

/**
 * An oscillator module that emulates a set of tone wheels for a drawbar organ.
 */
public class DrawbarOrgan extends Oscillator {
  /**
   * Creates a new drawbar organ with the given pitch and inputs for the level of each drawbar.
   */
  public DrawbarOrgan(FrequencyProvider frequency,
                      SignalProvider subFundamental,
                      SignalProvider subThirdHarmonic,
                      SignalProvider fundamental,
                      SignalProvider secondHarmonic,
                      SignalProvider thirdHarmonic,
                      SignalProvider fourthHarmonic,
                      SignalProvider fifthHarmonic,
                      SignalProvider sixthHarmonic,
                      SignalProvider eighthHarmonic) {
    super(frequency);

    depth_ = new SignalProvider[9];
    oscillator_ = new Sine[9];

    depth_[0] = subFundamental;
    oscillator_[0] = new Sine(new FrequencyProvider() {
      public double getLogFrequency(SynthesisTime time) {
        return frequency_.getLogFrequency(time) - 1.0;
      }
    });

    depth_[1] = subThirdHarmonic;
    oscillator_[1] = new Sine(new FrequencyProvider() {
      public double getLogFrequency(SynthesisTime time) {
        return frequency_.getLogFrequency(time) + 0.5849625;  // log2(3) =~ 7/12
      }
    });

    depth_[2] = fundamental;
    oscillator_[2] = new Sine(frequency);

    depth_[3] = secondHarmonic;
    oscillator_[3] = new Sine(new FrequencyProvider() {
      public double getLogFrequency(SynthesisTime time) {
        return frequency_.getLogFrequency(time) + 1.0;
      }
    });

    depth_[4] = thirdHarmonic;
    oscillator_[4] = new Sine(new FrequencyProvider() {
      public double getLogFrequency(SynthesisTime time) {
        return frequency_.getLogFrequency(time) + 1.5849625;  // 1 + log2(3) =~ 19/12
      }
    });

    depth_[5] = fourthHarmonic;
    oscillator_[5] = new Sine(new FrequencyProvider() {
      public double getLogFrequency(SynthesisTime time) {
        return frequency_.getLogFrequency(time) + 2.0;
      }
    });

    depth_[6] = fifthHarmonic;
    oscillator_[6] = new Sine(new FrequencyProvider() {
      public double getLogFrequency(SynthesisTime time) {
        return frequency_.getLogFrequency(time) + 2.3219281;  // 1 + log2(5) =~ 28/12;
      }
    });

    depth_[7] = sixthHarmonic;
    oscillator_[7] = new Sine(new FrequencyProvider() {
      public double getLogFrequency(SynthesisTime time) {
        return frequency_.getLogFrequency(time) + 2.5849625;  // 1 + log2(6) =~ 31/12;
      }
    });

    depth_[8] = eighthHarmonic;
    oscillator_[8] = new Sine(new FrequencyProvider() {
      public double getLogFrequency(SynthesisTime time) {
        return frequency_.getLogFrequency(time) + 3.0;
      }
    });
  }

  /**
   * Makes a weighted average of the value from each tone wheel.
   */
  public double computeValue(SynthesisTime time) {
    double output = 0.0;
    double denominator = 0.0;
    for (int i = 0; i < oscillator_.length; ++i) {
      double depth = depth_[i].getValue(time);
      if (depth != 0.0) {
        output += (depth * oscillator_[i].getValue(time));
        denominator += depth;
      }
    }
    if (denominator == 0.0) {
      return 0.0;
    }
    return output / denominator;
  }

  // How much each drawbar is pulled out.
  private SignalProvider[] depth_;

  // The set of underlying sine oscillators for each tone wheel.
  private Sine[] oscillator_;
}
