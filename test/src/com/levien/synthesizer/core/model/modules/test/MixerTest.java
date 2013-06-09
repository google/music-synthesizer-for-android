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

package com.levien.synthesizer.core.model.modules.test;

import com.levien.synthesizer.core.model.SynthesisTime;
import com.levien.synthesizer.core.model.SynthesizerInput;
import com.levien.synthesizer.core.model.modules.Mixer;

import junit.framework.TestCase;

public class MixerTest extends TestCase {
  final private static double TOLERANCE = 0.000001;

  public void setUp() {
    time_ = new SynthesisTime();
    time_.setSampleRate(44100.0);

    // Just set the inputs to arbitrary values.
    signal1_ = new SynthesizerInput(3.0, 0.0, 1.0);
    signal2_ = new SynthesizerInput(4.5, 0.0, 1.0);
    balance_ = new SynthesizerInput(0.0, 0.0, 1.0);

    mixer_ = new Mixer(signal1_, signal2_, balance_);
  }

  public void testChannel1() {
    balance_.setValue(0.0);
    assertEquals(3.0, mixer_.getValue(time_), TOLERANCE);
  }

  public void testChannel2() {
    balance_.setValue(1.0);
    assertEquals(4.5, mixer_.getValue(time_), TOLERANCE);
  }

  public void testMix() {
    balance_.setValue(0.5);
    assertEquals(3.75, mixer_.getValue(time_), TOLERANCE);
  }

  private SynthesisTime time_;
  private SynthesizerInput signal1_;
  private SynthesizerInput signal2_;
  private SynthesizerInput balance_;
  private Mixer mixer_;
}
