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

package com.levien.synthesizer.core.soundfont;

import java.util.Arrays;
import java.util.logging.Logger;

import com.levien.synthesizer.core.model.Envelope;
import com.levien.synthesizer.core.model.FrequencyProvider;
import com.levien.synthesizer.core.model.SynthesisTime;
import com.levien.synthesizer.core.model.oscillator.Oscillator;
import com.levien.synthesizer.core.music.Note;
import com.levien.synthesizer.core.soundfont.Zone.SampleMode;

/**
 * An oscillator module that outputs a sample from a file.
 */
public class SoundFontOscillator extends Oscillator implements Envelope {
  /**
   * Creates a new oscillator from a preset in a SoundFont file.
   * @param frequency - The frequency determines what key was pressed.
   * @parma preset - The SoundFont preset to get the sample data from.
   * @param sampleRateInHz - The sample rate of the output synthesizer.
   */
  public SoundFontOscillator(FrequencyProvider frequency,
                             Preset preset,
                             double sampleRateInHz) {
    super(frequency);
    logger_ = Logger.getLogger(getClass().getName());
    preset_ = preset;
    sampleRateInHz_ = sampleRateInHz;

    // Find the max sample length.
    maxSampleLength_ = 1;
    for (Zone pzone : preset_.getZoneList()) {
      Instrument instrument = pzone.getInstrument();
      if (instrument != null) {
        for (Zone izone : instrument.getZoneList()) {
          Sample sample = izone.getSample();
          int length = (int)Math.ceil((sampleRateInHz_ * izone.getCount()) / sample.getRate());
          if (length > maxSampleLength_) {
            maxSampleLength_ = length;
          }
        }
      }
    }

    currentSample_ = null;
    currentSampleIndex_ = 0;

    buffer_ = new double[maxSampleLength_];
    Arrays.fill(buffer_, 0.0);
    bufferIndex_ = 0;
  }

  /**
   * Gets one value from the synthesizer.
   */
  public synchronized double computeValue(SynthesisTime time) {
    // How many samples have passed?
    double deltaTime = time.getAbsoluteTime() - previousTime_;
    previousTime_ = time.getAbsoluteTime();

    // First, skip over however many samples we've passed in the buffer.
    int bufferSamples = (int)(deltaTime * sampleRateInHz_ + 0.5);
    // If everything in the buffer would be skipped, don't do extra work.
    if (bufferSamples > buffer_.length) {
      bufferSamples = buffer_.length;
    }
    // Clear all of the ones we've passed.
    for (int i = 0; i < bufferSamples; ++i) {
      buffer_[bufferIndex_++] = 0.0;
      if (bufferIndex_ >= buffer_.length) {
        bufferIndex_ = 0;
      }
    }
    double output = buffer_[bufferIndex_];

    // If the instrument has been retriggered, find the right sample.
    if (pressed_) {
      pressed_ = false;
      int key = Note.getKeyforLog12TET(frequency_.getLogFrequency(time));
      int velocity = 255;
      initSample(key, velocity);
    } else {
      if (currentSample_ != null) {
        // Advance by deltaTime in the current sample.
        currentSampleIndex_ += deltaTime * currentSample_.getSample().getRate();
        if (currentSample_.getSampleMode() == SampleMode.NO_LOOP) {
          if (currentSampleIndex_ > currentSample_.getEnd()) {
            // We're off the end of the sample.
            currentSample_ = null;
          }
        } else {
          if (currentSampleIndex_ >= currentSample_.getEndLoop()) {
            double distanceFromLoopStart = currentSampleIndex_ - currentSample_.getStartLoop();
            double loopLength = currentSample_.getEndLoop() - currentSample_.getStartLoop();          
            currentSampleIndex_ =
                currentSample_.getStartLoop() + (distanceFromLoopStart % loopLength);
          }
        }
      }
    }

    // Okay, now get the new data from the current sample.
    if (currentSample_ != null) {
      int firstSample = (int)currentSampleIndex_;
      double weight = 1.0 - (currentSampleIndex_ - firstSample);
      if (weight == 1.0) {
        output += currentSample_.getSample().getSample(firstSample);
      } else {
        output +=
            weight * currentSample_.getSample().getSample(firstSample) +
            (1.0 - weight) * currentSample_.getSample().getSample(firstSample + 1);
      }
    }

    return output;
  }

  /**
   * Initializes the "current sample" based on the given key and velocity.
   */
  private synchronized void initSample(int key, int velocity) {
    // Find any sample in this preset that can handle the given key and velocity.
    currentSample_ = null;
    for (Zone pzone : preset_.getZoneList()) {
      Instrument instrument = pzone.getInstrument();
      if (instrument != null) {
        for (Zone izone : instrument.getZoneList()) {
          if (izone.inKeyRange(key) &&
              izone.inVelocityRange(velocity) &&
              izone.getSample() != null) {
            currentSample_ = izone;
            currentSampleIndex_ = currentSample_.getStart();
            return;
          }
        }
      }
    }
  }

  /**
   * Trigger this oscillator to start outputting from the sample.
   */
  public synchronized void turnOn(boolean retriggerIfOn) {
    pressed_ = true;
  }
  
  /**
   * Turns off this oscillator from getting data from the current sample.
   * If this sample supports it, then the rest of the current sample is copied to the output buffer
   * before the current sample is cleared.
   */
  public synchronized void turnOff() {
    if (currentSample_ != null) {
      // Copy in the rest of the current sample.
      int bufferIndex = bufferIndex_;
      while (currentSampleIndex_ <= currentSample_.getEnd()) {
        int firstSample = (int)currentSampleIndex_;
        double weight = 1.0 - (currentSampleIndex_ - firstSample);
        if (weight == 1.0) {
          buffer_[bufferIndex] += currentSample_.getSample().getSample(firstSample);
        } else {
          buffer_[bufferIndex] +=
              weight * currentSample_.getSample().getSample(firstSample) +
              (1.0 - weight) * currentSample_.getSample().getSample(firstSample + 1);
        }
        bufferIndex = (bufferIndex + 1) % buffer_.length;
        currentSampleIndex_ += (currentSample_.getSample().getRate() / sampleRateInHz_);
      }
    }
    currentSample_ = null;
  }

  @SuppressWarnings("unused")
  private Logger logger_;

  // The preset to get samples from.
  private Preset preset_;

  // The current sample being played, and where in the sample we are currently at in the output.
  private Zone currentSample_;
  private double currentSampleIndex_;

  // A buffer of data that has been queued up and should be played in the future.  This handles the
  // case of releasing the key and then pressing it again at a different value.
  private double[] buffer_;  
  private int bufferIndex_;

  // True if the oscillator has been triggered, but the sample hasn't been loaded yet.
  private boolean pressed_;

  // The previous time the oscillator was polled for its value.
  private double previousTime_;

  // The maximum length of any sample in this preset, converted to the output sample rate.
  private int maxSampleLength_;
  
  // The sample rate of the output synthesizer.
  private double sampleRateInHz_;
}
