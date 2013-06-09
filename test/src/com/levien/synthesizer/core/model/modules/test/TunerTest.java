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
import com.levien.synthesizer.core.model.modules.Tuner;
import com.levien.synthesizer.core.music.Note;

import junit.framework.TestCase;

public class TunerTest extends TestCase {
  final private static double TOLERANCE = 0.000001;

  public void setUp() {
    time_ = new SynthesisTime();
    time_.setSampleRate(5.0);  // 5 Hz.

    // Just set the inputs to arbitrary values.
    source_ = new SynthesizerInput(3.0, 0.0, 1.0);
    shift_ = new SynthesizerInput(0.0, 0.0, 1.0);

    tuner_ = new Tuner(source_, shift_);
  }

  public void testOff() {
    source_.setValue(Note.computeLog12TET(Note.C, 3));
    shift_.setValue(0.0);
    assertEquals(Note.computeLog12TET(Note.C, 3), tuner_.getLogFrequency(time_), TOLERANCE);
  }

  public void testHalfStepUp() {
    source_.setValue(Note.computeLog12TET(Note.C, 3));
    shift_.setValue(Note.HALF_STEP);
    assertEquals(Note.computeLog12TET(Note.C_SHARP, 3), tuner_.getLogFrequency(time_), TOLERANCE);
  }

  public void testWholeStepUp() {
    source_.setValue(Note.computeLog12TET(Note.C, 3));
    shift_.setValue(Note.WHOLE_STEP);
    assertEquals(Note.computeLog12TET(Note.D, 3), tuner_.getLogFrequency(time_), TOLERANCE);
  }

  public void testHalfStepDown() {
    source_.setValue(Note.computeLog12TET(Note.C, 3));
    shift_.setValue(-1 * Note.HALF_STEP);
    assertEquals(Note.computeLog12TET(Note.B, 2), tuner_.getLogFrequency(time_), TOLERANCE);
  }

  public void testWholeStepDown() {
    source_.setValue(Note.computeLog12TET(Note.C, 3));
    shift_.setValue(-1 * Note.WHOLE_STEP);
    assertEquals(Note.computeLog12TET(Note.B_FLAT, 2), tuner_.getLogFrequency(time_), TOLERANCE);
  }

  private SynthesisTime time_;
  private SynthesizerInput source_;
  private SynthesizerInput shift_;
  private Tuner tuner_;
}
