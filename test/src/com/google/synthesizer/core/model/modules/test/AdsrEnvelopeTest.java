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

import junit.framework.TestCase;

import com.google.synthesizer.core.model.SynthesisTime;
import com.google.synthesizer.core.model.SynthesizerInput;
import com.google.synthesizer.core.model.modules.AdsrEnvelope;

public class AdsrEnvelopeTest extends TestCase {
  final private static double TOLERANCE = 0.0001;

  public void setUp() {
    time_ = new SynthesisTime();
    time_.setSampleRate(44100.0);  // 44.1 kHz sampling.

    // Set some arbitrary but realistic time parameters.
    attack_ = new SynthesizerInput(0.1, 0.0, 1.0);
    decay_ = new SynthesizerInput(0.2, 0.0, 1.0);
    sustain_ = new SynthesizerInput(0.8, 0.0, 1.0);
    release_ = new SynthesizerInput(0.3, 0.0, 1.0);

    adsr_ = new AdsrEnvelope(attack_, decay_, sustain_, release_);
  }

  /**
   * Checks that the attack matches the slope of its parameter.
   * @param duration - How long to check the slope.
   */
  private void checkA(double duration) {
    double startLevel = adsr_.getValue(time_);
    double start = time_.getAbsoluteTime();
    double finish = start + duration;
    double slope = 1.0 / attack_.getSynthesizerInputValue();
    for (; time_.getAbsoluteTime() <= finish; time_.advance()) {
      double x = time_.getAbsoluteTime() - start;
      double level = startLevel + x * slope;
      assertEquals(level, adsr_.getValue(time_), TOLERANCE);
    }
  }

  /**
   * Checks that the decay matches the slope of its parameter.
   * @param duration - How long to check the slope.
   */
  private void checkD(double duration) {
    double startLevel = adsr_.getValue(time_);
    double start = time_.getAbsoluteTime();
    double finish = start + duration;
    double slope = (sustain_.getSynthesizerInputValue() - 1.0) / decay_.getSynthesizerInputValue();
    for (; time_.getAbsoluteTime() <= finish; time_.advance()) {
      double x = time_.getAbsoluteTime() - start;
      double level = startLevel + x * slope;
      assertEquals(level, adsr_.getValue(time_), TOLERANCE);
    }
  }

  /**
   * Checks that the enveleope's output is sustain.
   * @param duration - How long to check the output.
   */
  private void checkS(double duration) {
    double start = time_.getAbsoluteTime();
    double finish = start + duration;
    for (; time_.getAbsoluteTime() <= finish; time_.advance()) {
      assertEquals(sustain_.getSynthesizerInputValue(), adsr_.getValue(time_), TOLERANCE);
    }
  }

  /**
   * Checks that the release matches the slope of its parameter.
   * @param duration - How long to check the slope.
   */
  private void checkR(double duration) {
    double startLevel = adsr_.getValue(time_);
    double start = time_.getAbsoluteTime();
    double finish = start + duration;
    double slope = (0.0 - sustain_.getSynthesizerInputValue()) /
        release_.getSynthesizerInputValue();
    for (; time_.getAbsoluteTime() <= finish; time_.advance()) {
      double x = time_.getAbsoluteTime() - start;
      double level = startLevel + x * slope;
      if (level < 0.0) {
        level = 0.0;
      }
      assertEquals(level, adsr_.getValue(time_), TOLERANCE);
    }
  }

  public void testTurnOffDuringAttack() {
    double time_On = 0.05;
    adsr_.turnOn(true);
    checkA(time_On);
    adsr_.turnOff();
    assertEquals(time_On / attack_.getSynthesizerInputValue(), adsr_.getValue(time_), TOLERANCE);
    checkR(release_.getSynthesizerInputValue());
    assertEquals(0.0, adsr_.getValue(time_), TOLERANCE);
  }

  public void testTurnOffDuringDecay() {
    double time_On = 0.2;
    adsr_.turnOn(true);
    assertEquals(0.0, adsr_.getValue(time_), TOLERANCE);
    checkA(attack_.getSynthesizerInputValue());
    assertEquals(1.0, adsr_.getValue(time_), TOLERANCE);
    checkD(time_On - attack_.getSynthesizerInputValue());
    adsr_.turnOff();
    checkR(release_.getSynthesizerInputValue() / sustain_.getSynthesizerInputValue());
    assertEquals(0.0, adsr_.getValue(time_), TOLERANCE);
  }

  public void testTurnOffDuringSustain() {
    double sustainDuration = 0.4;
    adsr_.turnOn(true);
    assertEquals(0.0, adsr_.getValue(time_), TOLERANCE);
    checkA(attack_.getSynthesizerInputValue());
    assertEquals(1.0, adsr_.getValue(time_), TOLERANCE);
    checkD(decay_.getSynthesizerInputValue());
    assertEquals(sustain_.getSynthesizerInputValue(), adsr_.getValue(time_), TOLERANCE);
    checkS(sustainDuration);
    adsr_.turnOff();
    assertEquals(sustain_.getSynthesizerInputValue(), adsr_.getValue(time_), TOLERANCE);
    checkR(release_.getSynthesizerInputValue());
    assertEquals(0.0, adsr_.getValue(time_), TOLERANCE);
  }

  public void testTriggerDuringAttack() {
    double attack1Duration = 0.05;
    double sustainDuration = 0.4;
    adsr_.turnOn(true);
    assertEquals(0.0, adsr_.getValue(time_), TOLERANCE);
    checkA(attack1Duration);
    adsr_.turnOn(true);
    checkA(attack_.getSynthesizerInputValue() - time_.getAbsoluteTime());
    assertEquals(1.0, adsr_.getValue(time_), TOLERANCE);
    checkD(decay_.getSynthesizerInputValue());
    assertEquals(sustain_.getSynthesizerInputValue(), adsr_.getValue(time_), TOLERANCE);
    checkS(sustainDuration);
    adsr_.turnOff();
    assertEquals(sustain_.getSynthesizerInputValue(), adsr_.getValue(time_), TOLERANCE);
    checkR(release_.getSynthesizerInputValue());
    assertEquals(0.0, adsr_.getValue(time_), TOLERANCE);
  }

  public void testTriggerDuringDecay() {
    double decayDuration = 0.15;
    double sustainDuration = 0.5;
    adsr_.turnOn(true);
    assertEquals(0.0, adsr_.getValue(time_), TOLERANCE);
    checkA(attack_.getSynthesizerInputValue());
    assertEquals(1.0, adsr_.getValue(time_), TOLERANCE);
    checkD(decayDuration);
    adsr_.turnOn(true);
    checkA(attack_.getSynthesizerInputValue() * (1.0 - adsr_.getValue(time_)));
    assertEquals(1.0, adsr_.getValue(time_), TOLERANCE);
    checkD(decay_.getSynthesizerInputValue());
    assertEquals(sustain_.getSynthesizerInputValue(), adsr_.getValue(time_), TOLERANCE);
    checkS(sustainDuration);
    adsr_.turnOff();
    assertEquals(sustain_.getSynthesizerInputValue(), adsr_.getValue(time_), TOLERANCE);
    checkR(release_.getSynthesizerInputValue());
    assertEquals(0.0, adsr_.getValue(time_), TOLERANCE);
  }

  public void testTriggerDuringSustain() {
    double sustainDuration1 = 0.5;
    double sustainDuration2 = 1.0;
    adsr_.turnOn(true);
    assertEquals(0.0, adsr_.getValue(time_), TOLERANCE);
    checkA(attack_.getSynthesizerInputValue());
    assertEquals(1.0, adsr_.getValue(time_), TOLERANCE);
    checkD(decay_.getSynthesizerInputValue());
    assertEquals(sustain_.getSynthesizerInputValue(), adsr_.getValue(time_), TOLERANCE);
    checkS(sustainDuration1);
    adsr_.turnOn(true);
    checkA(attack_.getSynthesizerInputValue() * (1.0 - sustain_.getSynthesizerInputValue()));
    assertEquals(1.0, adsr_.getValue(time_), TOLERANCE);
    checkD(decay_.getSynthesizerInputValue());
    assertEquals(sustain_.getSynthesizerInputValue(), adsr_.getValue(time_), TOLERANCE);
    checkS(sustainDuration2);
    adsr_.turnOff();
    assertEquals(sustain_.getSynthesizerInputValue(), adsr_.getValue(time_), TOLERANCE);
    checkR(release_.getSynthesizerInputValue());
    assertEquals(0.0, adsr_.getValue(time_), TOLERANCE);
  }

  public void testTriggerDuringRelease() {
    double releaseDuration1 = 0.1;
    double sustainDuration1 = 0.5;
    double sustainDuration2 = 1.0;
    adsr_.turnOn(true);
    assertEquals(0.0, adsr_.getValue(time_), TOLERANCE);
    checkA(attack_.getSynthesizerInputValue());
    assertEquals(1.0, adsr_.getValue(time_), TOLERANCE);
    checkD(decay_.getSynthesizerInputValue());
    assertEquals(sustain_.getSynthesizerInputValue(), adsr_.getValue(time_), TOLERANCE);
    checkS(sustainDuration1);
    adsr_.turnOff();
    assertEquals(sustain_.getSynthesizerInputValue(), adsr_.getValue(time_), TOLERANCE);
    checkR(releaseDuration1);
    adsr_.turnOn(true);
    checkA(attack_.getSynthesizerInputValue() * (1.0 - adsr_.getValue(time_)));
    assertEquals(1.0, adsr_.getValue(time_), TOLERANCE);
    checkD(decay_.getSynthesizerInputValue());
    assertEquals(sustain_.getSynthesizerInputValue(), adsr_.getValue(time_), TOLERANCE);
    checkS(sustainDuration2);
    adsr_.turnOff();
    assertEquals(sustain_.getSynthesizerInputValue(), adsr_.getValue(time_), TOLERANCE);
    checkR(release_.getSynthesizerInputValue());
    assertEquals(0.0, adsr_.getValue(time_), TOLERANCE);
  }

  public void testZeroAttack() {
    double sustainDuration = 5.0;
    attack_.setValue(0.0);
    sustain_.setValue(0.0);
    release_.setValue(0.0);

    adsr_.turnOn(true);
    assertEquals(0.0, adsr_.getValue(time_), TOLERANCE);
    time_.advance();
    assertEquals(1.0, adsr_.getValue(time_), TOLERANCE);
    checkD(decay_.getSynthesizerInputValue());
    assertEquals(0.0, adsr_.getValue(time_), TOLERANCE);
    checkS(sustainDuration);
    adsr_.turnOff();
    assertEquals(0.0, adsr_.getValue(time_), TOLERANCE);
  }

  private AdsrEnvelope adsr_;
  private SynthesisTime time_;
  private SynthesizerInput attack_;
  private SynthesizerInput decay_;
  private SynthesizerInput sustain_;
  private SynthesizerInput release_;
}
