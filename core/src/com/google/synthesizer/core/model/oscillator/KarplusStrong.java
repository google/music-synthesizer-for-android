/*
 * Copyright 2011 Google Inc.
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

package com.google.synthesizer.core.model.oscillator;

import com.google.synthesizer.core.model.Envelope;
import com.google.synthesizer.core.model.FrequencyProvider;
import com.google.synthesizer.core.model.SignalProvider;
import com.google.synthesizer.core.model.SynthesisTime;
import java.util.Arrays;

/**
 * Karplus-Strong string/drum synthesis algorithm.
 * 
 * @param frequency - The frequency of the note produced.
 * @param blend - Blend factor. Closer to 0 sounds more like a string, closer to 1 more like a drum.
 * @param stretch - How fast the vibration of the string should decay.
 * @param excitement - How noisy (i.e. random) the initial pluck should be.
 * @param sampleRateInHz - The sample rate, used with pitch to compute KS buffer size.
 */
public class KarplusStrong extends Oscillator implements Envelope {
  public KarplusStrong(FrequencyProvider frequency,
                       SignalProvider blend,
                       SignalProvider stretch,
                       SignalProvider excitement,
                       double sampleRateInHz) {
    super(frequency);
    sampleRateInHz_ = sampleRateInHz;
    buffer_ = new double[kMaxBufferSize];
    Arrays.fill(buffer_, 0.0);
    bufferSize_ = 0;
    current_ = 0;

    blend_ = blend;
    stretch_ = stretch;
    excitement_ = excitement;
  }

  @Override
  protected synchronized double computeValue(SynthesisTime time) {
    if (current_ == 0) {
      // We've looped around to the beginning of the buffer, so take the opportunity to resize.
      bufferSize_ =
        (int)Math.round(sampleRateInHz_ / (Math.pow(2.0, frequency_.getLogFrequency(time))));
      if (bufferSize_ > kMaxBufferSize) {
        bufferSize_ = kMaxBufferSize;
      }
    }

    // If the frequency is so high the buffer disappears, then we just can't do anything.
    if (bufferSize_ == 0) {
      trigger_ = false;
      return 0.0;
    }

    // The "string" has been "plucked".  Fill the buffer with noise.
    if (trigger_) {
      trigger_ = false;
      double excitement = excitement_.getValue(time);
      for (int i = 0; i < kMaxBufferSize; ++i) {
        if (excitement == 0.0) {
          // Just an optimization.
          buffer_[i] = 0.5;
        } else {
          buffer_[i] = excitement * (Math.random() * 2.0 - 1.0);
        }
      }
    }

    // Process the buffer.
    double output = buffer_[current_];
    double blend = blend_.getValue(time);
    double stretch = 1.0 - stretch_.getValue(time);  // Invert stretch so it's more intuitive.
    double direction = (blend == 0.0 || (blend != 1.0 && blend <= Math.random())) ? -1.0 : 1.0;
    double magnitude = 0.0;
    if (stretch == 0.0 || (stretch != 1.0 && stretch <= Math.random())) {
      magnitude = buffer_[current_];
    } else {
      magnitude = (buffer_[current_] + previousOutput_) / 2.0;
    }
    buffer_[current_] = direction * magnitude;
    previousOutput_ = output;
    current_ = (current_ + 1) % bufferSize_;
    return output;
  }

  /**
   * Doesn't really do anything at this time.
   */
  public synchronized void turnOff() {
    gate_ = false;
  }

  /**
   * Causes the string to be plucked by setting trigger_ to true.
   */
  public synchronized void turnOn(boolean retriggerIfOn) {
    if (gate_ && !retriggerIfOn) {
      return;
    }
    trigger_ = true;
    gate_ = true;
  }

  private double[] buffer_;
  private int bufferSize_;
  private double sampleRateInHz_;
  private int current_;
  private double previousOutput_;

  // Blend factor.  Closer to 0 sounds more like a string.  Closer to 1 sounds more like a drum.
  private SignalProvider blend_;

  // How fast the vibration of the string should decay.
  private SignalProvider stretch_;

  // How noisy (i.e. random) the initial pluck should be.
  private SignalProvider excitement_;

  private boolean trigger_;
  private boolean gate_;

  // The max buffer size is the highest sample rate divided by the lowest frequency (20 Hz).
  private final static int kMaxBufferSize = 44100/20;
}
