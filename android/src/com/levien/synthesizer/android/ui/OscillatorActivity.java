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

package com.levien.synthesizer.android.ui;

import android.os.Bundle;
import com.levien.synthesizer.R;
import com.levien.synthesizer.core.model.composite.MultiChannelSynthesizer;
import com.levien.synthesizer.core.model.composite.Presets.Setting;
import com.levien.synthesizer.android.widgets.knob.KnobView;
import com.levien.synthesizer.android.widgets.piano.PianoView;
import com.levien.synthesizer.android.widgets.waveform.WaveformRowView;

/**
 * Activity for modifying oscillator parameters.
 * TODO(klimt): Add the ability to switch channels.
 */
public class OscillatorActivity extends SynthesizerActivity {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.oscillator);

    piano_ = (PianoView)findViewById(R.id.piano);
    waveformView_ = (WaveformRowView)findViewById(R.id.waveform);
    glideKnob_ = (KnobView)findViewById(R.id.glideKnob);
    coarseKnob_ = (KnobView)findViewById(R.id.coarseKnob);
    fineKnob_ = (KnobView)findViewById(R.id.fineKnob);
    vibratoDepthKnob_ = (KnobView)findViewById(R.id.vibratoDepthKnob);
    balanceKnob_ = (KnobView)findViewById(R.id.balanceKnob);
  }

  @Override
  protected void onSynthesizerUpdate(MultiChannelSynthesizer synth) {
    int channel = getIntentChannel(this);
    Setting[] settings = getIntentSettings(this);
    piano_.bindTo(synthesizer_, channel);
    waveformView_.bindTo(synthesizer_, channel, settings[0]);      
    glideKnob_.bindTo(synthesizer_, channel, settings[1]);
    coarseKnob_.bindTo(synthesizer_, channel, settings[2]);
    fineKnob_.bindTo(synthesizer_, channel, settings[3]);
    vibratoDepthKnob_.bindTo(synthesizer_, channel, settings[4]);
    balanceKnob_.bindTo(synthesizer_, channel, settings[5]);
  }

  private PianoView piano_;
  private WaveformRowView waveformView_;
  private KnobView glideKnob_;
  private KnobView coarseKnob_;
  private KnobView fineKnob_;
  private KnobView vibratoDepthKnob_;
  private KnobView balanceKnob_;
}
