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
import com.levien.synthesizer.core.model.modules.Echo;

import junit.framework.TestCase;

public class EchoTest extends TestCase {
  final private static double TOLERANCE = 0.000001;

  public void testBasic() {
    // Test the echo effect sampling at 10 Hz with a delay of 0.5 seconds.
    // This means input should repeat every 5 samples.

    final double MIX = 0.6;
    final double DELAY_IN_SECONDS = 0.5;
    final double SAMPLE_RATE = 10.0;
    final int DELAY_IN_SAMPLES = (int)Math.round(DELAY_IN_SECONDS * SAMPLE_RATE);

    assertTrue(DELAY_IN_SAMPLES > 0);

    SynthesisTime time = new SynthesisTime();
    time.setSampleRate(44100.0);
    SynthesizerInput signal = new SynthesizerInput(0.0, 0.0, 1.0);
    SynthesizerInput mix = new SynthesizerInput(MIX, 0.0, 1.0);
    SynthesizerInput delay = new SynthesizerInput(DELAY_IN_SECONDS, 0.0, 1.0);
    Echo echo = new Echo(signal, mix, delay, SAMPLE_RATE);

    // It's only really interesting when input is longer than DELAY_IN_SAMPLES.
    // It's also good for its length to not be a multiple of DELAY_IN_SAMPLES.
    double[] input = { 0.3, 0.1, 0.4, 0.1, 0.5, 0.9, 0.2, 0.6 };

    for (int i = 0; i < 1000; ++i) {
      if (i < input.length) {
        signal.setValue(input[i]);
      } else {
        signal.setValue(0.0);
      }

      double expected = 0.0;
      // Loop backwards through time and sum up the residuals.
      for (int j = 0; j <= i / DELAY_IN_SAMPLES; ++j) {
        int index = i - j * DELAY_IN_SAMPLES;
        if (index < input.length) {
          expected += (input[index] * (1.0 - MIX) * Math.pow(MIX, j));
        }
      }

      assertEquals(expected, echo.getValue(time), TOLERANCE);
      time.advance();
    }
  }
}
