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

package com.levien.synthesizer.core.model.oscillator.test;

import com.levien.synthesizer.core.model.SynthesisTime;
import com.levien.synthesizer.core.model.SynthesizerInput;
import com.levien.synthesizer.core.model.oscillator.Triangle;

import junit.framework.TestCase;

public class TriangleTest extends TestCase {
  final private static double TOLERANCE = 0.0000001;

  public void testBasic() {
    SynthesizerInput frequency = new SynthesizerInput(0.0, 0.0, 1.0);  // 1 Hz.

    SynthesisTime time = new SynthesisTime();
    time.setSampleRate(8);  // 8 samples per second.
    Triangle triangle = new Triangle(frequency);

    assertEquals(1.0/2.0, triangle.getValue(time), TOLERANCE);
    time.advance();
    assertEquals(0.0/2.0, triangle.getValue(time), TOLERANCE);
    time.advance();
    assertEquals(-1.0/2.0, triangle.getValue(time), TOLERANCE);
    time.advance();
    assertEquals(-2.0/2.0, triangle.getValue(time), TOLERANCE);
    time.advance();
    assertEquals(-1.0/2.0, triangle.getValue(time), TOLERANCE);
    time.advance();
    assertEquals(0.0/2.0, triangle.getValue(time), TOLERANCE);
    time.advance();
    assertEquals(1.0/2.0, triangle.getValue(time), TOLERANCE);
    time.advance();
    assertEquals(2.0/2.0, triangle.getValue(time), TOLERANCE);
    time.advance();
    assertEquals(1.0/2.0, triangle.getValue(time), TOLERANCE);
  }

  public void testFrequencyChange() {
    SynthesizerInput frequency = new SynthesizerInput(0.0, 0.0, 1.0);  // 1 Hz.

    SynthesisTime time = new SynthesisTime();
    time.setSampleRate(8);  // 8 samples per second.
    Triangle triangle = new Triangle(frequency);

    assertEquals(1.0/2.0, triangle.getValue(time), TOLERANCE);
    time.advance();
    assertEquals(0.0/2.0, triangle.getValue(time), TOLERANCE);
    time.advance();

    // Change the frequency.
    frequency.setValue(-1.0);  // 1/2 Hz.

    assertEquals(-1.0/2.0, triangle.getValue(time), TOLERANCE);
    time.advance();
    assertEquals(-2.0/2.0, triangle.getValue(time), TOLERANCE);
    time.advance();

    // The frequency change takes effect here at the end of the cycle.

    assertEquals(-3.0/4.0, triangle.getValue(time), TOLERANCE);
    time.advance();
    assertEquals(-2.0/4.0, triangle.getValue(time), TOLERANCE);
    time.advance();
    assertEquals(-1.0/4.0, triangle.getValue(time), TOLERANCE);
    time.advance();
    assertEquals(0.0/4.0, triangle.getValue(time), TOLERANCE);
    time.advance();
    assertEquals(1.0/4.0, triangle.getValue(time), TOLERANCE);
    time.advance();
    assertEquals(2.0/4.0, triangle.getValue(time), TOLERANCE);
    time.advance();
    assertEquals(3.0/4.0, triangle.getValue(time), TOLERANCE);
    time.advance();
    assertEquals(4.0/4.0, triangle.getValue(time), TOLERANCE);
    time.advance();
    assertEquals(3.0/4.0, triangle.getValue(time), TOLERANCE);
  }
}
