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

package com.levien.synthesizer.core.model.test;

import com.levien.synthesizer.core.model.SynthesisTime;
import com.levien.synthesizer.core.model.SynthesizerInput;

import junit.framework.TestCase;

public class SynthesizerInputTest extends TestCase {
  final private static double TOLERANCE = 0.000001;

  public void testGetSynthesizerInputValue() {
    SynthesizerInput input = new SynthesizerInput(3.2, 0.0, 1.0);
    assertEquals(3.2, input.getSynthesizerInputValue(), TOLERANCE);
  }

  public void testGetValue() {
    SynthesisTime time = new SynthesisTime();
    time.setSampleRate(44100.0);

    SynthesizerInput input = new SynthesizerInput(3.5, 1.0, 5.0);
    assertEquals(3.5, input.getValue(time), TOLERANCE);
    time.advance();
    assertEquals(3.5, input.getValue(time), TOLERANCE);

    input.setByteValue((byte)0);
    assertEquals(1.0, input.getValue(time), TOLERANCE);

    input.setByteValue((byte)63);
    assertEquals(3.0, input.getValue(time), 0.03);

    input.setByteValue((byte)127);
    assertEquals(5.0, input.getValue(time), TOLERANCE);
  }

  public void testGetLogFrequency() {
    SynthesisTime time = new SynthesisTime();
    time.setSampleRate(44100.0);

    SynthesizerInput input = new SynthesizerInput(7.1, 0.0, 1.0);
    assertEquals(7.1, input.getLogFrequency(time), TOLERANCE);
    time.advance();
    assertEquals(7.1, input.getLogFrequency(time), TOLERANCE);
  }

  public void testChange() {
    SynthesisTime time = new SynthesisTime();
    time.setSampleRate(44100.0);

    SynthesizerInput input = new SynthesizerInput(5.5, 0.0, 1.0);
    assertEquals(5.5, input.getSynthesizerInputValue(), TOLERANCE);
    assertEquals(5.5, input.getValue(time), TOLERANCE);
    assertEquals(5.5, input.getLogFrequency(time), TOLERANCE);
    time.advance();
    assertEquals(5.5, input.getSynthesizerInputValue(), TOLERANCE);
    assertEquals(5.5, input.getValue(time), TOLERANCE);
    assertEquals(5.5, input.getLogFrequency(time), TOLERANCE);

    // Change the value.
    input.setValue(3.2);
    assertEquals(3.2, input.getSynthesizerInputValue(), TOLERANCE);
    assertEquals(3.2, input.getValue(time), TOLERANCE);
    assertEquals(3.2, input.getLogFrequency(time), TOLERANCE);
    time.advance();
    assertEquals(3.2, input.getSynthesizerInputValue(), TOLERANCE);
    assertEquals(3.2, input.getValue(time), TOLERANCE);
    assertEquals(3.2, input.getLogFrequency(time), TOLERANCE);
  }
}
