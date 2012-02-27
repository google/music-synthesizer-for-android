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

package com.google.synthesizer.core.model.composite;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.logging.Logger;

import com.google.protobuf.TextFormat;
import com.google.synthesizer.core.midi.MidiAdapter;
import com.google.synthesizer.core.model.SignalProvider;
import com.google.synthesizer.core.model.SynthesisTime;
import com.google.synthesizer.core.model.SynthesizerInput;
import com.google.synthesizer.core.model.composite.Presets.PresetLibrary;
import com.google.synthesizer.core.model.composite.Presets.Setting;
import com.google.synthesizer.core.soundfont.SoundFontReader;

/**
 * MultiChannelSynthesizer is an array of MidiSynthesizer.
 */
public class MultiChannelSynthesizer extends MidiAdapter implements SignalProvider {
  /**
   * MultiChannelSynthesizer is an array of BasicSynths.
   * @param channels - The number of channels of the synthesizer.
   * @param fingers - How many fingers to support.
   * @param sampleRateInHz - The sample rate of the underlying BasicSynths.
   */
  public MultiChannelSynthesizer(int channels, int fingers, double sampleRateInHz,
                                 SoundFontReader sampleProvider) {
    logger_ = Logger.getLogger(getClass().getName());
    synth_ = new MidiSynthesizer[channels];
    for (int i = 0; i < synth_.length; ++i) {
      synth_[i] = new MidiSynthesizer(fingers, sampleRateInHz, sampleProvider);
    }
  }

  /**
   * Returns the output of the synthesizer.
   */
  public double getValue(SynthesisTime time) {
    double value = 0.0;
    for (int i = 0; i < synth_.length; ++i) {
      value += synth_[i].getValue(time);
    }
    return value;
  }

  /**
   * Called to turn on the given note for the given channel.
   */
  @Override
  public void onNoteOn(int channel, int note, int velocity) {
    synth_[channel].onNoteOn(note, velocity);
  }

  /**
   * Called to turn off the given note for the given channel.
   */
  @Override
  public void onNoteOff(int channel, int note, int velocity) {
    synth_[channel].onNoteOff(note, velocity);
  }

  /**
   * Called when a control value changes on the given channel.
   */
  @Override
  public void onController(int channel, int control, int value) {
    MidiSynthesizer synth = synth_[channel];
    Setting setting = Setting.valueOf(control);
    SynthesizerInput input = synth.getSynthesizerInput(setting);
    if (input != null) {
      input.setByteValue((byte)value);
      logger_.warning(
          "Processed control: " + setting.name() + "[" + channel + "] = " + value + ".");
    } else {
      logger_.warning("Unhandled control: " + control + "[" + channel + "] = " + value + ".");
    }
  }

  /**
   * Called when the program is changed.
   */
  @Override
  public void onProgramChange(int channel, int program) {
    synth_[channel].setPreset(program);
  }

  /**
   * Populates names with the list of all known presets from the library.
   * Does *not* clear the list first.
   */
  public void getPresetNames(ArrayList<String> names) {
    synth_[0].getPresetNames(names);
  }

  /**
   * Populates library with the presets from the given input stream, in text protocol buffer format.
   */
  public void loadLibraryFromText(InputStream input) throws IOException {
    PresetLibrary.Builder builder = PresetLibrary.newBuilder();
    TextFormat.merge(new InputStreamReader(input), builder);
    PresetLibrary library = builder.build();
    for (int i = 0; i < synth_.length; ++i) {
      synth_[i].setLibrary(library);
      // Load each channel with the next available preset.
      synth_[i].setPreset(i % synth_[i].getPresetCount());
    }
  }

  /**
   * Loads a channel with the settings from the preset in the library with the given index.
   */
  public void setPreset(int channel, int index) {
    synth_[channel].setPreset(index);
  }

  /**
   * Returns the MidiSynthesizer to use for a particular channel.
   */
  public MidiSynthesizer getChannel(int channel) {
    return synth_[channel];
  }

  // The actual synthesizers.
  private MidiSynthesizer[] synth_;

  Logger logger_;
}
