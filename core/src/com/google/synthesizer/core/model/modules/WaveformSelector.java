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

package com.google.synthesizer.core.model.modules;

import java.util.ArrayList;

import com.google.synthesizer.core.model.FrequencyProvider;
import com.google.synthesizer.core.model.SignalProvider;
import com.google.synthesizer.core.model.SynthesisTime;
import com.google.synthesizer.core.model.WaveformInput;
import com.google.synthesizer.core.model.oscillator.Noise;
import com.google.synthesizer.core.model.oscillator.Sawtooth;
import com.google.synthesizer.core.model.oscillator.Sine;
import com.google.synthesizer.core.model.oscillator.Square;
import com.google.synthesizer.core.model.oscillator.Triangle;

/**
 * Module that outputs a waveform from a selectable set of oscillators.
 */
public class WaveformSelector implements SignalProvider {
  /**
   * Constructs a waveform selector with no waveforms available.
   * @param waveform - The source for which waveform to use at any given time.
   */
  public WaveformSelector(WaveformInput waveform) {
    waveform_ = waveform;
    sources_ = new ArrayList<SignalProvider>();
  }

  /**
   * Adds a source for a new waveform type.
   */
  public synchronized void addWaveform(String waveform, SignalProvider source) {
    int id = waveform_.addWaveform(waveform);
    while (sources_.size() < id + 1) {
      sources_.add(null);
    }
    sources_.set(id, source);
  }

  /**
   * Returns the output of the source associated with the selected waveform.
   */
  public synchronized double getValue(SynthesisTime time) {
    int selected = waveform_.getSelected();
    SignalProvider provider = sources_.get(selected);
    if (provider != null) {
      return provider.getValue(time);
    } else {
      return 0.0;
    }
  }

  /**
   * Adds the standard waveforms to this selector.
   */
  public synchronized void addDefaultWaveforms(FrequencyProvider frequency) {
    addWaveform(WaveformInput.SINE, new Sine(frequency));
    addWaveform(WaveformInput.TRIANGLE, new Triangle(frequency));
    addWaveform(WaveformInput.SQUARE, new Square(frequency));
    addWaveform(WaveformInput.SAWTOOTH, new Sawtooth(frequency));
    addWaveform(WaveformInput.NOISE, new Noise(frequency));
  }

  // Instances of each waveform type.
  private ArrayList<SignalProvider> sources_;

  // The currently selected waveform.
  private WaveformInput waveform_;
}
