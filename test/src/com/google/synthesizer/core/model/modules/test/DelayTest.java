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

package com.google.synthesizer.core.model.modules.test;

import com.google.synthesizer.core.model.SynthesisTime;
import com.google.synthesizer.core.model.SynthesizerInput;
import com.google.synthesizer.core.model.modules.Delay;

import junit.framework.TestCase;

public class DelayTest extends TestCase {
  final private static double TOLERANCE = 0.000001;

  public void testBasic() {
    SynthesisTime time = new SynthesisTime();
    time.setSampleRate(44100.0);
    SynthesizerInput signal = new SynthesizerInput(1.0, 0.0, 1.0);
    SynthesizerInput mix = new SynthesizerInput(0.5, 0.0, 1.0);
    Delay delay = new Delay(signal, mix);

    // Regular mode.

    signal.setValue(3.0);
    assertEquals(1.5, delay.getValue(time), TOLERANCE);
    signal.setValue(1.0);
    assertEquals(1.5, delay.getValue(time), TOLERANCE);
    time.advance();
    assertEquals(0.5, delay.getValue(time), TOLERANCE);
    signal.setValue(4.0);
    assertEquals(0.5, delay.getValue(time), TOLERANCE);
    time.advance();
    assertEquals(2.0, delay.getValue(time), TOLERANCE);
    signal.setValue(1.0);
    assertEquals(2.0, delay.getValue(time), TOLERANCE);
    time.advance();

    // Record mode.

    delay.startRecording();
    signal.setValue(3.0);
    assertEquals(1.5, delay.getValue(time), TOLERANCE);
    signal.setValue(1.0);
    assertEquals(1.5, delay.getValue(time), TOLERANCE);
    time.advance();
    assertEquals(0.5, delay.getValue(time), TOLERANCE);
    signal.setValue(4.0);
    assertEquals(0.5, delay.getValue(time), TOLERANCE);
    time.advance();
    assertEquals(2.0, delay.getValue(time), TOLERANCE);
    signal.setValue(1.0);
    assertEquals(2.0, delay.getValue(time), TOLERANCE);
    time.advance();

    // Play mode.

    delay.startPlaying();
    signal.setValue(6.0);
    assertEquals(3.0 + 1.5, delay.getValue(time), TOLERANCE);
    assertEquals(3.0 + 1.5, delay.getValue(time), TOLERANCE);
    time.advance();
    assertEquals(3.0 + 0.5, delay.getValue(time), TOLERANCE);
    assertEquals(3.0 + 0.5, delay.getValue(time), TOLERANCE);
    time.advance();
    assertEquals(3.0 + 2.0, delay.getValue(time), TOLERANCE);
    assertEquals(3.0 + 2.0, delay.getValue(time), TOLERANCE);
    time.advance();

    // Looped.

    signal.setValue(7.0);
    assertEquals(3.5 + 1.5, delay.getValue(time), TOLERANCE);
    assertEquals(3.5 + 1.5, delay.getValue(time), TOLERANCE);
    time.advance();
    assertEquals(3.5 + 0.5, delay.getValue(time), TOLERANCE);
    assertEquals(3.5 + 0.5, delay.getValue(time), TOLERANCE);
    time.advance();
    assertEquals(3.5 + 2.0, delay.getValue(time), TOLERANCE);
    assertEquals(3.5 + 2.0, delay.getValue(time), TOLERANCE);
    time.advance();

    // Regular mode.

    delay.stopPlaying();
    assertEquals(3.5, delay.getValue(time), TOLERANCE);
    assertEquals(3.5, delay.getValue(time), TOLERANCE);
    time.advance();
    assertEquals(3.5, delay.getValue(time), TOLERANCE);
    signal.setValue(9.0);
    assertEquals(3.5, delay.getValue(time), TOLERANCE);
    time.advance();
    assertEquals(4.5, delay.getValue(time), TOLERANCE);
    assertEquals(4.5, delay.getValue(time), TOLERANCE);
    time.advance();
  }
}
