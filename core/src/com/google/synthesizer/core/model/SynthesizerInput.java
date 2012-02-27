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
 * SynthesizerInput provides a frequency or signal that doesn't inherently change over time.
 * It can change occasionally as the user turns a knob or whatever, but doesn't change on its own.
 */
public class SynthesizerInput implements FrequencyProvider, SignalProvider {
  /**
   * Creates a new SynthesizerInput that will return value as both a frequency and a signal.
   */
  public SynthesizerInput(double value, double min, double max) {
    value_ = value;
    min_ = min;
    max_ = max;
  }

  /**
   * Returns the input value as a log of frequency.
   * @return the value.
   */
  public synchronized double getLogFrequency(SynthesisTime time) {
    return value_;
  }

  /**
   * Returns the input value as a signal.
   * @return the value.
   */
  public synchronized double getValue(SynthesisTime time) {
    return value_;
  }

  /**
   * Returns the input value.
   * @return the value.
   */
  public synchronized double getSynthesizerInputValue() {
    return value_;
  }
  
  /**
   * Sets the input to a new value.
   * @param value - the new value.
   */
  public synchronized void setValue(double value) {
    value_ = value;
  }

  /**
   * Sets the input based on an unsigned byte value in the range 0 to 127.
   */
  public synchronized void setByteValue(byte value) {
    value_ = min_ + (value / 127.0) * (max_ - min_);
  }

  private double min_;
  private double max_;
  private double value_;
}
