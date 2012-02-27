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

package com.google.synthesizer.core.model;

import java.util.ArrayList;

/**
 * WaveformInput provides a waveform type that is selectable.
 */
public class WaveformInput {
  /**
   * Creates a new WaveformInput with the given initial value.
   */
  public WaveformInput(String waveform) {
    selected_ = 0;
    waveforms_ = new ArrayList<String>();
    waveforms_.add(waveform);
  }

  /**
   * @return the index of the given waveform.
   */
  public synchronized int getWaveformIndex(String waveform) {
    for (int i = 0; i < waveforms_.size(); ++i) {
      if (waveforms_.get(i).equals(waveform)) {
        return i;
      }
    }
    return -1;
  }
  
  /**
   * Selects a waveform.
   */
  public synchronized void select(String waveform) {
    selected_ = getWaveformIndex(waveform);
    if (selected_ < 0) {
      selected_ = 0;
    }
  }

  /**
   * Select the next available waveform.
   */
  public synchronized void next() {
    selected_ = (selected_ + 1) % waveforms_.size();
  }

  /**
   * Select the previous available waveform.
   */
  public synchronized void previous() {
    if (selected_ == 0) {
      selected_ = waveforms_.size() - 1;
    } else {
      --selected_;
    }
  }

  /**
   * Returns the currently selected waveform.
   */
  public synchronized int getSelected() {
    return selected_;
  }

  /**
   * Adds a new waveform to this input.
   * @return The index of the new waveform.
   */
  public synchronized int addWaveform(String waveform) {
    int id = getWaveformIndex(waveform);
    if (id < 0) {
      id = waveforms_.size();
      waveforms_.add(waveform);
    }
    return id;
  }

  /**
   * @return The number of waveforms that are selectable.
   */
  public synchronized int getWaveformCount() {
    return waveforms_.size();
  }

  /**
   * @return The identifier of the waveform with the given index.
   */
  public synchronized String getWaveform(int i) {
    return waveforms_.get(i);
  }

  // The currently selected waveform.
  private int selected_;

  // The set of all available waveforms on this input.
  private ArrayList<String> waveforms_;

  // A few default built-in strings.
  public static String SINE = "sine";
  public static String TRIANGLE = "triangle";
  public static String SQUARE = "square";
  public static String SAWTOOTH = "sawtooth";
  public static String NOISE = "noise";
  public static String KARPLUS_STRONG = "karplus-strong string";
  public static String DRAWBAR_ORGAN = "drawbar organ";
  public static String DRUMS = "drums";
}
