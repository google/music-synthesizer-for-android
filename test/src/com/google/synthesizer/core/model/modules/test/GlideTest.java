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
import com.google.synthesizer.core.model.modules.Glide;

import junit.framework.TestCase;

public class GlideTest extends TestCase {
  final private static double TOLERANCE = 0.000001;

  public void setUp() {
    time_ = new SynthesisTime();
    time_.setSampleRate(5.0);  // 5 Hz.

    // Just set the inputs to arbitrary values.
    source_ = new SynthesizerInput(3.0, 0.0, 1.0);
    rate_ = new SynthesizerInput(0.0, 0.0, 1.0);

    glide_ = new Glide(source_, rate_);
  }

  public void testOff() {
    rate_.setValue(0.0);

    source_.setValue(3.0);
    assertEquals(3.0, glide_.getLogFrequency(time_), TOLERANCE);
    time_.advance();
    source_.setValue(4.0);
    assertEquals(4.0, glide_.getLogFrequency(time_), TOLERANCE);
  }

  public void testBasic() {
    source_.setValue(3.0);
    assertEquals(3.0, glide_.getLogFrequency(time_), TOLERANCE);
    time_.advance();

    rate_.setValue(1.0);
    assertEquals(3.0, glide_.getLogFrequency(time_), TOLERANCE);
    time_.advance();

    source_.setValue(4.0);
    assertEquals(3.0, glide_.getLogFrequency(time_), TOLERANCE);
    time_.advance();
    assertEquals(3.2, glide_.getLogFrequency(time_), TOLERANCE);
    time_.advance();
    assertEquals(3.4, glide_.getLogFrequency(time_), TOLERANCE);
    time_.advance();
    assertEquals(3.6, glide_.getLogFrequency(time_), TOLERANCE);
    time_.advance();
    assertEquals(3.8, glide_.getLogFrequency(time_), TOLERANCE);
    time_.advance();
    assertEquals(4.0, glide_.getLogFrequency(time_), TOLERANCE);
    time_.advance();
    assertEquals(4.0, glide_.getLogFrequency(time_), TOLERANCE);
  }

  private SynthesisTime time_;
  private SynthesizerInput source_;
  private SynthesizerInput rate_;
  private Glide glide_;
}
