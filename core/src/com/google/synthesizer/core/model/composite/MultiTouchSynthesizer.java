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

package com.google.synthesizer.core.model.composite;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.synthesizer.core.model.CachedSignalProvider;
import com.google.synthesizer.core.model.Envelope;
import com.google.synthesizer.core.model.FrequencyProvider;
import com.google.synthesizer.core.model.SignalProvider;
import com.google.synthesizer.core.model.SynthesisTime;
import com.google.synthesizer.core.model.SynthesizerInput;
import com.google.synthesizer.core.model.WaveformInput;
import com.google.synthesizer.core.model.composite.Presets.Preset;
import com.google.synthesizer.core.model.composite.Presets.PresetLibrary;
import com.google.synthesizer.core.model.composite.Presets.Setting;
import com.google.synthesizer.core.model.modules.AdsrEnvelope;
import com.google.synthesizer.core.model.modules.Amplifier;
import com.google.synthesizer.core.model.modules.Delay;
import com.google.synthesizer.core.model.modules.Echo;
import com.google.synthesizer.core.model.modules.Glide;
import com.google.synthesizer.core.model.modules.LowPassFilter;
import com.google.synthesizer.core.model.modules.Mixer;
import com.google.synthesizer.core.model.modules.Tremolo;
import com.google.synthesizer.core.model.modules.Tuner;
import com.google.synthesizer.core.model.modules.WaveformSelector;
import com.google.synthesizer.core.model.oscillator.DrawbarOrgan;
import com.google.synthesizer.core.model.oscillator.KarplusStrong;
import com.google.synthesizer.core.soundfont.SoundFontOscillator;
import com.google.synthesizer.core.soundfont.SoundFontReader;

/**
 * MultiTouchSynthesizer is a collection of synthesizer modules, connected together in a way similar
 * to many basic analog synthesizers.
 */
public class MultiTouchSynthesizer implements SignalProvider {
  /**
   * Creates a new synthesizer with default settings.
   * @param sampleRateInHz - Sample rate, used to compute buffer size from delay time.
   */
  public MultiTouchSynthesizer(int fingers, double sampleRateInHz, SoundFontReader sampleLibrary) {
    FINGERS = fingers;
    setup(sampleRateInHz, sampleLibrary);
  }

  /**
   * Connects all of the modules together.
   * @param sampleRateInHz - Sample rate, used to compute buffer size from delay time.
   */
  @SuppressWarnings("unchecked")
  private void setup(double sampleRateInHz, SoundFontReader sampleLibrary) {
    // Data Structures.
    library_ = PresetLibrary.newBuilder().build();
    pitch_ = new SynthesizerInput[FINGERS];
    synthesizerInputs_ = new HashMap<Setting, SynthesizerInput>();
    waveformInputs_ = new HashMap<Setting, WaveformInput>();
    envelopes_ = new ArrayList[FINGERS];
    for (int i = 0; i < FINGERS; ++i) {
      pitch_[i] = new SynthesizerInput(0.0, 4.0, 15.0);
      envelopes_[i] = new ArrayList<Envelope>();
    }

    // Vibrato
    SignalProvider[] vibrato = setupVibrato();

    // Oscillator 1
    SignalProvider[] oscillator1 = setupOscillator(Setting.OSCILLATOR_1_GLIDE,
                                                   Setting.OSCILLATOR_1_COARSE,
                                                   Setting.OSCILLATOR_1_FINE,
                                                   Setting.OSCILLATOR_1_VIBRATO,
                                                   Setting.OSCILLATOR_1_WAVEFORM,
                                                   Setting.OSCILLATOR_1_BLEND,
                                                   Setting.OSCILLATOR_1_STRETCH,
                                                   Setting.OSCILLATOR_1_EXCITEMENT,
                                                   pitch_,
                                                   vibrato,
                                                   true,
                                                   sampleLibrary,
                                                   sampleRateInHz);
    // Oscillator 2
    SignalProvider[] oscillator2 = setupOscillator(Setting.OSCILLATOR_2_GLIDE,
                                                   Setting.OSCILLATOR_2_COARSE,
                                                   Setting.OSCILLATOR_2_FINE,
                                                   Setting.OSCILLATOR_2_VIBRATO,
                                                   Setting.OSCILLATOR_2_WAVEFORM,
                                                   Setting.OSCILLATOR_2_BLEND,
                                                   Setting.OSCILLATOR_2_STRETCH,
                                                   Setting.OSCILLATOR_2_EXCITEMENT,
                                                   pitch_,
                                                   vibrato,
                                                   true,
                                                   sampleLibrary,
                                                   sampleRateInHz);

    // Mixing
    SynthesizerInput balance = new SynthesizerInput(0.0, 0.0, 1.0);
    synthesizerInputs_.put(Setting.BALANCE, balance);
    SignalProvider[] oscillatorOutput = new SignalProvider[FINGERS];
    for (int finger = 0; finger < FINGERS; ++finger) {
      oscillatorOutput[finger] = new Mixer(oscillator1[finger], oscillator2[finger], balance);
    }

    // Tremolo
    SignalProvider[] tremolo = setupTremolo(oscillatorOutput);

    // Low-pass Filter
    SignalProvider[] low_pass_filter = setupLowPassFilter(tremolo);

    // Amplifier
    SignalProvider[] envelope = setupEnvelope(Setting.ATTACK,
                                              Setting.DECAY,
                                              Setting.SUSTAIN,
                                              Setting.RELEASE);
    SynthesizerInput volume = new SynthesizerInput(1.0, 0.0, 25.0);
    synthesizerInputs_.put(Setting.VOLUME, volume);

    final SignalProvider[] ampOutput = new SignalProvider[FINGERS];
    for (int finger = 0; finger < FINGERS; ++finger) {
      SignalProvider amplification = new Amplifier(envelope[finger], volume);
      ampOutput[finger] = new Amplifier(low_pass_filter[finger], amplification);
    }

    // Merge the fingers.
    SignalProvider mergedOutput = new SignalProvider() {
      public double getValue(SynthesisTime time) {
        double output = 0.0;
        for (int finger = 0; finger < FINGERS; ++finger) {
          output += ((1.0 / FINGERS) * ampOutput[finger].getValue(time));
        }
        return output;
      }
    };
    
    // Effects
    SignalProvider echo = setupEcho(mergedOutput, sampleRateInHz);
    output_ = setupDelay(echo);
  }

  /**
   * Sets up an envelope.
   */
  private SignalProvider[] setupEnvelope(Setting attackSetting,
                                         Setting decaySetting,
                                         Setting sustainSetting,
                                         Setting releaseSetting) {
    SynthesizerInput attack = new SynthesizerInput(0.01, 0.01, 1.0);
    SynthesizerInput decay = new SynthesizerInput(0.01, 0.01, 1.0);
    SynthesizerInput sustain = new SynthesizerInput(1.0, 0.0, 1.0);
    SynthesizerInput release = new SynthesizerInput(0.01, 0.01, 1.0);

    synthesizerInputs_.put(attackSetting, attack);
    synthesizerInputs_.put(decaySetting, decay);
    synthesizerInputs_.put(sustainSetting, sustain);
    synthesizerInputs_.put(releaseSetting, release);

    SignalProvider[] response = new SignalProvider[FINGERS];
    for (int finger = 0; finger < FINGERS; ++finger) {
      Envelope envelope = new AdsrEnvelope(attack, decay, sustain, release);
      envelopes_[finger].add(envelope);
      response[finger] = envelope;
    }
    return response;
  }

  /**
   * Sets up the vibrato section of the synthesizer.
   */
  private SignalProvider[] setupVibrato() {
    SynthesizerInput rate = new SynthesizerInput(0.0, 0.0, 10.0);
    WaveformInput waveform = new WaveformInput(WaveformInput.SINE);
    synthesizerInputs_.put(Setting.VIBRATO_RATE, rate);
    waveformInputs_.put(Setting.VIBRATO_WAVEFORM, waveform);
    SignalProvider[] envelope = setupEnvelope(Setting.VIBRATO_ATTACK,
                                              Setting.VIBRATO_DECAY,
                                              Setting.VIBRATO_SUSTAIN,
                                              Setting.VIBRATO_RELEASE);
    SignalProvider[] response = new SignalProvider[FINGERS];
    for (int finger = 0; finger < FINGERS; ++finger) {
      WaveformSelector waveformOutput = new WaveformSelector(waveform);
      waveformOutput.addDefaultWaveforms(rate);    
      response[finger] = new Amplifier(waveformOutput, envelope[finger]);
    }
    return response;
  }

  /**
   * Sets up a Karplus-Strong string section of the synthesizer.
   */
  private SignalProvider[] setupKarplusStrong(Setting blendSetting,
                                              Setting stretchSetting,
                                              Setting excitementSetting,
                                              FrequencyProvider[] pitch,
                                              double sampleRateInHz) {
    SynthesizerInput blend = new SynthesizerInput(0.0, 0.0, 1.0);
    SynthesizerInput stretch = new SynthesizerInput(0.0, 0.0, 1.0);
    SynthesizerInput excitement = new SynthesizerInput(0.0, 0.0, 1.0);

    synthesizerInputs_.put(blendSetting, blend);
    synthesizerInputs_.put(stretchSetting, stretch);
    synthesizerInputs_.put(excitementSetting, excitement);

    SignalProvider[] response = new SignalProvider[FINGERS];
    for (int finger = 0; finger < FINGERS; ++finger) {
      KarplusStrong karplusStrong =
        new KarplusStrong(pitch[finger], blend, stretch, excitement, sampleRateInHz);
      envelopes_[finger].add(karplusStrong);
      response[finger] = karplusStrong;
    }
    return response;
  }

  /**
   * Sets up a drawbar organ osciallator for each finger.
   */
  private SignalProvider[] setupDrawbarOrgan(FrequencyProvider[] pitch) {
    SynthesizerInput drawbar1 = new SynthesizerInput(8.0/8.0, 0.0, 1.0);
    SynthesizerInput drawbar2 = new SynthesizerInput(8.0/8.0, 0.0, 1.0);
    SynthesizerInput drawbar3 = new SynthesizerInput(8.0/8.0, 0.0, 1.0);
    SynthesizerInput drawbar4 = new SynthesizerInput(8.0/8.0, 0.0, 1.0);
    SynthesizerInput drawbar5 = new SynthesizerInput(0.0/8.0, 0.0, 1.0);
    SynthesizerInput drawbar6 = new SynthesizerInput(0.0/8.0, 0.0, 1.0);
    SynthesizerInput drawbar7 = new SynthesizerInput(0.0/8.0, 0.0, 1.0);
    SynthesizerInput drawbar8 = new SynthesizerInput(0.0/8.0, 0.0, 1.0);
    SynthesizerInput drawbar9 = new SynthesizerInput(0.0/8.0, 0.0, 1.0);
    synthesizerInputs_.put(Setting.ORGAN_DRAWBAR_1, drawbar1);
    synthesizerInputs_.put(Setting.ORGAN_DRAWBAR_2, drawbar2);
    synthesizerInputs_.put(Setting.ORGAN_DRAWBAR_3, drawbar3);
    synthesizerInputs_.put(Setting.ORGAN_DRAWBAR_4, drawbar4);
    synthesizerInputs_.put(Setting.ORGAN_DRAWBAR_5, drawbar5);
    synthesizerInputs_.put(Setting.ORGAN_DRAWBAR_6, drawbar6);
    synthesizerInputs_.put(Setting.ORGAN_DRAWBAR_7, drawbar7);
    synthesizerInputs_.put(Setting.ORGAN_DRAWBAR_8, drawbar8);
    synthesizerInputs_.put(Setting.ORGAN_DRAWBAR_9, drawbar9);
    SignalProvider[] response = new SignalProvider[FINGERS];
    for (int finger = 0; finger < FINGERS; ++finger) {
      DrawbarOrgan organ = new DrawbarOrgan(pitch[finger],
                                            drawbar1,
                                            drawbar2,
                                            drawbar3,
                                            drawbar4,
                                            drawbar5,
                                            drawbar6,
                                            drawbar7,
                                            drawbar8,
                                            drawbar9);
      response[finger] = organ;
    }
    return response;
  }

  /**
   * Sets up a sampled drum.
   */
  private SignalProvider[] setupDrums(FrequencyProvider[] pitch,
                                      double sampleRateInHz,
                                      SoundFontReader sampleLibrary) {
    SignalProvider[] response = new SignalProvider[FINGERS];
    for (int finger = 0; finger < FINGERS; ++finger) {
      SoundFontOscillator sample = new SoundFontOscillator(
          pitch[finger],
          sampleLibrary.getPresets().get(sampleLibrary.getPresets().size() - 1),
          sampleRateInHz);
      response[finger] = sample;
      envelopes_[finger].add(sample);
    }
    return response;
  }

  /**
   * Sets up an oscillator section of the synthesizer.
   */
  private SignalProvider[] setupOscillator(Setting glideSetting,
                                           Setting coarseSetting,
                                           Setting fineSetting,
                                           Setting vibratoSetting,
                                           Setting waveformSetting,
                                           Setting blendSetting,
                                           Setting stretchSetting,
                                           Setting excitementSetting,
                                           FrequencyProvider[] pitch,
                                           SignalProvider[] vibrato,
                                           boolean includeOrgan,
                                           SoundFontReader sampleLibrary,
                                           double sampleRateInHz) {
    SynthesizerInput glide = new SynthesizerInput(0.0, 0.0, 1.0);
    SynthesizerInput coarse = new SynthesizerInput(0.0, -1.0, 1.0);
    SynthesizerInput fine = new SynthesizerInput(0.0, -0.0833333333, 0.0833333333);
    SynthesizerInput vibratoDepth = new SynthesizerInput(0.0, 0.0, 0.1666666667);
    WaveformInput waveform = new WaveformInput(WaveformInput.SINE);

    // Register all of the inputs.
    synthesizerInputs_.put(glideSetting, glide);
    synthesizerInputs_.put(coarseSetting, coarse);
    synthesizerInputs_.put(fineSetting, fine);
    synthesizerInputs_.put(vibratoSetting, vibratoDepth);
    waveformInputs_.put(waveformSetting, waveform);

    // Create a KarplusStrong module.
    SignalProvider[] karplusStrong =
      setupKarplusStrong(blendSetting, stretchSetting, excitementSetting, pitch, sampleRateInHz);

    // Create an organ module.
    SignalProvider[] organ = null;
    if (includeOrgan) {
      organ = setupDrawbarOrgan(pitch);
    }

    // Create a drum module.
    SignalProvider[] drums = null;
    if (sampleLibrary != null) {
      drums = setupDrums(pitch, sampleRateInHz, sampleLibrary);
    }

    SignalProvider[] response = new SignalProvider[FINGERS];
    for (int finger = 0; finger < FINGERS; ++finger) {
      // Apply all of the layers that can control pitch;
      FrequencyProvider adjustedPitch = new Glide(pitch[finger], glide);
      adjustedPitch = new Tuner(adjustedPitch, coarse);
      adjustedPitch = new Tuner(adjustedPitch, fine);
      adjustedPitch = new Tuner(adjustedPitch, new Amplifier(vibrato[finger], vibratoDepth));

      // Create the waveform.
      WaveformSelector selector = new WaveformSelector(waveform);
      selector.addDefaultWaveforms(adjustedPitch);
      selector.addWaveform(WaveformInput.KARPLUS_STRONG, karplusStrong[finger]);
      if (organ != null) {
        selector.addWaveform(WaveformInput.DRAWBAR_ORGAN, organ[finger]);
      }
      if (drums != null) {
        selector.addWaveform(WaveformInput.DRUMS, drums[finger]);
      }
      response[finger] = selector;
    }
    return response;
  }

  /**
   * Sets up the tremolo section of the synthesizer.
   */
  private SignalProvider[] setupTremolo(SignalProvider[] source) {
    SynthesizerInput rate = new SynthesizerInput(0.0, 0.0, 10.0);
    SynthesizerInput depth = new SynthesizerInput(0.0, 0.0, 1.0);
    WaveformInput waveform = new WaveformInput(WaveformInput.SINE);

    synthesizerInputs_.put(Setting.TREMOLO_RATE, rate);
    synthesizerInputs_.put(Setting.TREMOLO_DEPTH, depth);
    waveformInputs_.put(Setting.TREMOLO_WAVEFORM, waveform);
    
    SignalProvider[] envelope = setupEnvelope(Setting.TREMOLO_ATTACK,
                                              Setting.TREMOLO_DECAY,
                                              Setting.TREMOLO_SUSTAIN,
                                              Setting.TREMOLO_RELEASE);

    SignalProvider[] response = new SignalProvider[FINGERS];
    for (int finger = 0; finger < FINGERS; ++finger) {
      WaveformSelector waveformOutput = new WaveformSelector(waveform);
      waveformOutput.addDefaultWaveforms(rate);
      response[finger] =
        new Amplifier(source[finger],
                      new Tremolo(new Amplifier(waveformOutput, envelope[finger]), depth));
    }
    return response;
  }

  /**
   * Sets up the filter section of the synthesizer.
   */
  private SignalProvider[] setupLowPassFilter(SignalProvider[] source) {
    final SynthesizerInput cutoff = new SynthesizerInput(1.0, 0.0, 1.0);
    final SynthesizerInput depth = new SynthesizerInput(0.0, -1.0, 1.0);

    synthesizerInputs_.put(Setting.FILTER_CUTOFF, cutoff);
    synthesizerInputs_.put(Setting.FILTER_DEPTH, depth);

    final SignalProvider[] envelope = setupEnvelope(Setting.FILTER_ATTACK,
                                                    Setting.FILTER_DECAY,
                                                    Setting.FILTER_SUSTAIN,
                                                    Setting.FILTER_RELEASE);

    SignalProvider[] response = new SignalProvider[FINGERS];
    for (int finger = 0; finger < FINGERS; ++finger) {
      final SignalProvider envelope_finger = envelope[finger];

      SignalProvider shapedCutoff = new CachedSignalProvider() {
        public synchronized double computeValue(SynthesisTime time) {
          double c = cutoff.getValue(time);
          double d = depth.getValue(time);
          // This fancy math makes the envelope behave like we want.
          double x = c * Math.abs(d) - 0.5 * (d + Math.abs(d));
          return c + x * (envelope_finger.getValue(time) - 1);
        }
      };  
      response[finger] = new LowPassFilter(source[finger], shapedCutoff);
    }
    return response;
  }

  /**
   * Sets up an echo module on the synthesizer.
   */
  private SignalProvider setupEcho(SignalProvider source, double sampleRateInHz) {
    SynthesizerInput mix = new SynthesizerInput(0.0, 0.0, 1.0);
    SynthesizerInput delay = new SynthesizerInput(0.0, 0.1, 2.0);

    synthesizerInputs_.put(Setting.ECHO_MIX, mix);
    synthesizerInputs_.put(Setting.ECHO_DELAY, delay);

    return new Echo(source, mix, delay, sampleRateInHz);
  }

  /**
   * Sets up the delay module for the synthesizer.
   */
  private SignalProvider setupDelay(SignalProvider source) {
    SynthesizerInput mix = new SynthesizerInput(0.5, 0.0, 1.0);
    synthesizerInputs_.put(Setting.DELAY_MIX, mix);
    delay_ = new Delay(source, mix);
    return delay_;
  }

  /**
   * Gets a particular input for this synthesizer by its control id.
   */
  public SynthesizerInput getSynthesizerInput(Setting setting) {
    if (synthesizerInputs_.containsKey(setting)) {
      return synthesizerInputs_.get(setting);
    }
    return null;
  }

  /**
   * Gets a particular input for this synthesizer by its control id.
   */
  public WaveformInput getWaveformInput(Setting setting) {
    if (waveformInputs_.containsKey(setting)) {
      return waveformInputs_.get(setting);
    }
    return null;
  }

  /**
   * Sets the input pitch of the synthesizer.
   * @param logFrequency - The log frequency of the pitch.
   */
  public void setPitch(double logFrequency, int finger) {
    if (finger < FINGERS) {
      pitch_[finger].setValue(logFrequency);
    }
  }

  /**
   * Returns the output of the synthesizer, as it should go to the speaker.
   */
  public double getValue(SynthesisTime time) {
    return output_.getValue(time);
  }

  /**
   * Turns on all envelopes used by the synth.
   * @param retriggerIfOn - A (hopefully temporary) hack.  Tells whether to treat this as a new
   *     press if the key is already down.
   */
  public void turnOn(boolean retriggerIfOn, int finger) {
    if (finger < FINGERS) {
      for (int i = 0; i < envelopes_[finger].size(); ++i) {
        envelopes_[finger].get(i).turnOn(retriggerIfOn);
      }
    }
  }

  /**
   * Turns off all envelopes used by the synth.
   */
  public void turnOff(int finger) {
    if (finger < FINGERS) {
      for (int i = 0; i < envelopes_[finger].size(); ++i) {
        envelopes_[finger].get(i).turnOff();
      }
    }
  }

  /**
   * Starts recording what the user plays.
   */
  public void startRecording() {
    delay_.startRecording();
  }

  /**
   * Stops recording.
   */
  public void stopRecording() {
    delay_.stopRecording();
  }

  /**
   * Starts playing what the user has recorded.
   */
  public void startPlaying() {
    delay_.startPlaying();
  }

  /**
   * Stops playing the recorded audio.
   */
  public void stopPlaying() {
    delay_.stopPlaying();
  }

  /**
   * Returns whether the user's recording is being played.
   */
  public boolean isPlaying() {
    return delay_.isPlaying();
  }

  /**
   * Returns whether the user's playing is being recorded.
   */
  public boolean isRecording() {
    return delay_.isRecording();
  }

  /**
   * Loads this synthesizer with the settings from the given preset.
   */
  public void setPreset(Preset preset) {
    for (int i = 0; i < preset.getInputSettingCount(); ++i) {
      SynthesizerInput input = getSynthesizerInput(preset.getInputSetting(i).getSetting());
      if (input != null) {
        input.setValue(preset.getInputSetting(i).getValue());
      } else {
        throw new RuntimeException(
            "Unable to set synthesizer input " + preset.getInputSetting(i).getSetting());
      }
    }
    for (int i = 0; i < preset.getWaveformSettingCount(); ++i) {
      WaveformInput input = getWaveformInput(preset.getWaveformSetting(i).getSetting());
      if (input != null) {
        input.select(preset.getWaveformSetting(i).getWaveform());
      } else {
        throw new RuntimeException(
            "Unable to set synthesizer input " + preset.getWaveformSetting(i).getSetting() + ".");
      }
    }
  }

  /**
   * Loads this synthesizer with the settings from the preset in the library with the given name.
   */
  public void setPreset(String name) {
    for (int i = 0; i < library_.getPresetCount(); ++i) {
      if (library_.getPreset(i).getName().equals(name)) {
        setPreset(library_.getPreset(i));
        return;
      }
    }
    throw new RuntimeException("Tried to load an unknown preset: \"" + name + "\".");
  }

  /**
   * Loads this synthesizer with the settings from the preset in the library with the given index.
   */
  public void setPreset(int index) {
    if (index < 0 || index >= library_.getPresetCount()) {
      throw new RuntimeException("Tried to load an unknown preset: " + index + ".");
    }
    setPreset(library_.getPreset(index));
  }

  /**
   * Returns the number of presets available in the library.
   */
  public int getPresetCount() {
    return library_.getPresetCount();
  }

  /**
   * Populates names with the list of all known presets from the library.
   * Does *not* clear the list first.
   */
  public void getPresetNames(ArrayList<String> names) {
    for (int i = 0; i < library_.getPresetCount(); ++i) {
      names.add(library_.getPreset(i).getName());
    }
  }

  public void setLibrary(PresetLibrary library) throws IOException {
    library_ = library;
  }

  /**
   * Return the maximum number of fingers that can be used.
   */
  public int getMaxFingerCount() {
    return FINGERS;
  }

  // Data structures for keeping track of input.
  private Map<Setting, SynthesizerInput> synthesizerInputs_;
  private Map<Setting, WaveformInput> waveformInputs_;

  // How many fingers this synthesizer supports.
  protected final int FINGERS;

  // Keyboard input.
  private SynthesizerInput[] pitch_;

  // List of envelopes that needs to be triggered when a key is pressed.
  private List<Envelope>[] envelopes_;

  //The delay module that handles recording and playback.
  private Delay delay_;

  //Synthesizer output.
  private SignalProvider output_;

  // A collection of setting presets.
  private PresetLibrary library_;
}
