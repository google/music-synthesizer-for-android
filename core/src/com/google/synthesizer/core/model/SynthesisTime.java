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

package com.google.synthesizer.core.model;

/**
 * A simple counter to keep track of the logical time in synthesis.
 */
public class SynthesisTime {
  /**
   * Creates a new timer with a 0 time delta.
   */
  public SynthesisTime() {
    deltaTime_ = 0.0;
    absoluteTime_ = 0.0;
  }

  /**
   * Returns how much time will elapse next time advance() is called.
   * @return the time delta in seconds.
   */
  public double getDeltaTime() {
    return deltaTime_;
  }

  /**
   * Returns the absolute time that has elapsed since creation or last reset().
   * @return the time in seconds.
   */
  public double getAbsoluteTime() {
    return absoluteTime_;
  }

  /**
   * Sets the "sample rate" of the synthesizer, which is the inverse of the time delta.
   * @param sampleRate - samples per second (in Hz).
   */
  public void setSampleRate(double sampleRate) {
    if (sampleRate == 0.0) {
      deltaTime_ = 0.0;
    } else {
      deltaTime_ = 1.0 / sampleRate;
    }
  }

  /**
   * Resets the absolute time to zero.
   */
  public void reset() {
    absoluteTime_ = 0.0;
  }

  /**
   * Advances the absolute time by the time delta.
   */
  public void advance() {
    absoluteTime_ += deltaTime_;
  }

  private double deltaTime_;
  private double absoluteTime_;
}
