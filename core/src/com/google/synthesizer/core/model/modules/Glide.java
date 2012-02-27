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

import com.google.synthesizer.core.model.CachedFrequencyProvider;
import com.google.synthesizer.core.model.FrequencyProvider;
import com.google.synthesizer.core.model.SignalProvider;
import com.google.synthesizer.core.model.SynthesisTime;

/**
 * Glide smooths changes in frequency over time.
 */
public class Glide extends CachedFrequencyProvider {
  /**
   * Creates a Glide module to wrap source.
   * @param source - The input frequency to smooth.
   * @param rate - The time in seconds in which output should change to a new input.
   */
  public Glide(FrequencyProvider source, SignalProvider rate) {
    source_ = source;
    rate_ = rate;
    previousLogFrequency_ = 0.0;
    nextLogFrequency_ = 0.0;
  }
  
  public double computeLogFrequency(SynthesisTime time) {
    double currentLogFrequency = source_.getLogFrequency(time);
    double rate = rate_.getValue(time);
    
    // If rate is 0 seconds, then don't do anything.
    if (rate == 0.0) {
      previousLogFrequency_ = currentLogFrequency;
      nextLogFrequency_ = currentLogFrequency;
      return currentLogFrequency;
    }

    // It hasn't changed, so don't do anything.
    if (currentLogFrequency == previousLogFrequency_ &&
        nextLogFrequency_ == previousLogFrequency_) {
      return currentLogFrequency;
    }

    // See where we are, based on where we were last heading.
    double timeSinceChange = time.getAbsoluteTime() - switchTime_;
    double output;
    if (timeSinceChange > rate) {
      output = nextLogFrequency_;
    } else {
      output = previousLogFrequency_ +
          (timeSinceChange / rate) * (nextLogFrequency_ - previousLogFrequency_);
    }

    // Adjust if there is a new destination.
    if (currentLogFrequency != nextLogFrequency_) {
      previousLogFrequency_ = output;
      nextLogFrequency_ = currentLogFrequency;
      switchTime_ = time.getAbsoluteTime();
    }
    
    return output;
  }

  // The wrapped frequency provider to take as input and output smoothed.
  private FrequencyProvider source_;

  // The time in seconds in which output should change to a new input.
  private SignalProvider rate_;

  // The log frequency that was being output last time the input switched.
  private double previousLogFrequency_;

  // The log frequency of the input that this module is moving toward.
  private double nextLogFrequency_;

  // The last time that in the input frequency changed.
  private double switchTime_;
}
