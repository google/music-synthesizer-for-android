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

import com.levien.synthesizer.core.model.SignalProvider;
import com.levien.synthesizer.core.model.SynthesisTime;
import com.levien.synthesizer.core.model.SynthesizerInput;
import com.levien.synthesizer.core.model.modules.Amplifier;

import junit.framework.TestCase;

public class AmplifierTest extends TestCase {
  final private static double TOLERANCE = 0.000001;

  public void testAmp() {
    SynthesisTime time = new SynthesisTime();
    time.setSampleRate(44100.0);
    SignalProvider signal = new SynthesizerInput(3.0, 0.0, 1.0);
    SignalProvider gain = new SynthesizerInput(4.5, 0.0, 1.0);

    Amplifier amp = new Amplifier(signal, gain);

    assertEquals(13.5, amp.getValue(time), TOLERANCE);
  }
}
