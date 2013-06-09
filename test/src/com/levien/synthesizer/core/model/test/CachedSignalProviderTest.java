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

import com.levien.synthesizer.core.model.CachedSignalProvider;
import com.levien.synthesizer.core.model.SynthesisTime;

import junit.framework.TestCase;

public class CachedSignalProviderTest extends TestCase {
  final private static double TOLERANCE = 0.000001;

  // Make a simple subclass that just increments the output value.
  private class MockCachedSignalProvider extends CachedSignalProvider {
    public MockCachedSignalProvider() {
      counter_ = 0;
    }
    protected double computeValue(SynthesisTime time) {
      return counter_++;
    }
    private int counter_;
  }

  // Check that the value doesn't change until the time is advanced.
  public void testGetValue() {
    SynthesisTime time = new SynthesisTime();
    time.setSampleRate(44100.0);

    MockCachedSignalProvider mock = new MockCachedSignalProvider();

    assertEquals(0, mock.getValue(time), TOLERANCE);
    assertEquals(0, mock.getValue(time), TOLERANCE);
    assertEquals(0, mock.getValue(time), TOLERANCE);

    time.advance();

    assertEquals(1, mock.getValue(time), TOLERANCE);
    assertEquals(1, mock.getValue(time), TOLERANCE);

    time.advance();

    assertEquals(2, mock.getValue(time), TOLERANCE);
    assertEquals(2, mock.getValue(time), TOLERANCE);
    assertEquals(2, mock.getValue(time), TOLERANCE);
    assertEquals(2, mock.getValue(time), TOLERANCE);
  }
}
