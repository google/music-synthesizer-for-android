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
import com.google.synthesizer.core.model.modules.Tremolo;

import junit.framework.TestCase;

public class TremoloTest extends TestCase {
  final private static double TOLERANCE = 0.000001;

  public void setUp() {
    time_ = new SynthesisTime();
    time_.setSampleRate(5.0);  // 5 Hz.

    // Just set the inputs to arbitrary values.
    modulator_ = new SynthesizerInput(3.0, 0.0, 1.0);
    depth_ = new SynthesizerInput(0.0, 0.0, 1.0);

    tremolo_ = new Tremolo(modulator_, depth_);
  }

  public void testOff() {
    depth_.setValue(0.0);

    modulator_.setValue(0.0);
    assertEquals(1.0, tremolo_.getValue(time_), TOLERANCE);
    time_.advance();

    modulator_.setValue(0.5);
    assertEquals(1.0, tremolo_.getValue(time_), TOLERANCE);
    time_.advance();

    modulator_.setValue(1.0);
    assertEquals(1.0, tremolo_.getValue(time_), TOLERANCE);
    time_.advance();

    modulator_.setValue(0.5);
    assertEquals(1.0, tremolo_.getValue(time_), TOLERANCE);
    time_.advance();

    modulator_.setValue(0.0);
    assertEquals(1.0, tremolo_.getValue(time_), TOLERANCE);
    time_.advance();

    modulator_.setValue(-0.5);
    assertEquals(1.0, tremolo_.getValue(time_), TOLERANCE);
    time_.advance();

    modulator_.setValue(-1.0);
    assertEquals(1.0, tremolo_.getValue(time_), TOLERANCE);
    time_.advance();

    modulator_.setValue(-0.5);
    assertEquals(1.0, tremolo_.getValue(time_), TOLERANCE);
    time_.advance();

    modulator_.setValue(0.0);
    assertEquals(1.0, tremolo_.getValue(time_), TOLERANCE);
    time_.advance();
  }

  public void testHalf() {
    depth_.setValue(0.5);

    modulator_.setValue(0.0);
    assertEquals(0.75, tremolo_.getValue(time_), TOLERANCE);
    time_.advance();

    modulator_.setValue(0.5);
    assertEquals(0.875, tremolo_.getValue(time_), TOLERANCE);
    time_.advance();

    modulator_.setValue(1.0);
    assertEquals(1.0, tremolo_.getValue(time_), TOLERANCE);
    time_.advance();

    modulator_.setValue(0.5);
    assertEquals(0.875, tremolo_.getValue(time_), TOLERANCE);
    time_.advance();

    modulator_.setValue(0.0);
    assertEquals(0.75, tremolo_.getValue(time_), TOLERANCE);
    time_.advance();

    modulator_.setValue(-0.5);
    assertEquals(0.625, tremolo_.getValue(time_), TOLERANCE);
    time_.advance();

    modulator_.setValue(-1.0);
    assertEquals(0.5, tremolo_.getValue(time_), TOLERANCE);
    time_.advance();

    modulator_.setValue(-0.5);
    assertEquals(0.625, tremolo_.getValue(time_), TOLERANCE);
    time_.advance();

    modulator_.setValue(0.0);
    assertEquals(0.75, tremolo_.getValue(time_), TOLERANCE);
    time_.advance();
  }

  public void testFull() {
    depth_.setValue(1.0);

    modulator_.setValue(0.0);
    assertEquals(0.5, tremolo_.getValue(time_), TOLERANCE);
    time_.advance();

    modulator_.setValue(0.5);
    assertEquals(0.75, tremolo_.getValue(time_), TOLERANCE);
    time_.advance();

    modulator_.setValue(1.0);
    assertEquals(1.0, tremolo_.getValue(time_), TOLERANCE);
    time_.advance();

    modulator_.setValue(0.5);
    assertEquals(0.75, tremolo_.getValue(time_), TOLERANCE);
    time_.advance();

    modulator_.setValue(0.0);
    assertEquals(0.5, tremolo_.getValue(time_), TOLERANCE);
    time_.advance();

    modulator_.setValue(-0.5);
    assertEquals(0.25, tremolo_.getValue(time_), TOLERANCE);
    time_.advance();

    modulator_.setValue(-1.0);
    assertEquals(0.0, tremolo_.getValue(time_), TOLERANCE);
    time_.advance();

    modulator_.setValue(-0.5);
    assertEquals(0.25, tremolo_.getValue(time_), TOLERANCE);
    time_.advance();

    modulator_.setValue(0.0);
    assertEquals(0.5, tremolo_.getValue(time_), TOLERANCE);
    time_.advance();
  }

  private SynthesisTime time_;
  private SynthesizerInput modulator_;
  private SynthesizerInput depth_;
  private Tremolo tremolo_;
}
