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

import com.levien.synthesizer.core.model.CachedSignalProvider;
import com.levien.synthesizer.core.model.FrequencyProvider;

/**
  * Subclass for any oscillator -- any module that outputs a waveform based on a given frequency.
  * The range of the output is [-1, 1.0].
  */
public abstract class Oscillator extends CachedSignalProvider {
  /**
   * Constructor for subclasses to store the frequency.
   */
  public Oscillator(FrequencyProvider frequency) {
    frequency_ = frequency;
  }

  protected FrequencyProvider frequency_;
}
