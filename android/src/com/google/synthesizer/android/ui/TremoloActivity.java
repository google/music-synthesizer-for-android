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

package com.google.synthesizer.android.ui;

import android.os.Bundle;
import com.google.synthesizer.R;
import com.google.synthesizer.android.widgets.knob.KnobView;
import com.google.synthesizer.android.widgets.piano.PianoView;
import com.google.synthesizer.android.widgets.waveform.WaveformRowView;
import com.google.synthesizer.core.model.composite.MultiChannelSynthesizer;
import com.google.synthesizer.core.model.composite.Presets.Setting;

/**
 * Activity for modifying tremolo.
 * TODO(klimt): Add the ability to switch channels.
 */
public class TremoloActivity extends SynthesizerActivity {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.tremolo);

    piano_ = (PianoView)findViewById(R.id.piano);
    waveformView_ = (WaveformRowView)findViewById(R.id.waveform);
    rateKnob_ = (KnobView)findViewById(R.id.rateKnob);
    depthKnob_ = (KnobView)findViewById(R.id.depthKnob);
    attackKnob_ = (KnobView)findViewById(R.id.attackKnob);
    decayKnob_ = (KnobView)findViewById(R.id.decayKnob);
    sustainKnob_ = (KnobView)findViewById(R.id.sustainKnob);
    releaseKnob_ = (KnobView)findViewById(R.id.releaseKnob);
  }

  @Override
  protected void onSynthesizerUpdate(MultiChannelSynthesizer synth) {
    int channel = getIntentChannel(this);
    piano_.bindTo(synthesizer_, channel);
    waveformView_.bindTo(synthesizer_, channel, Setting.TREMOLO_WAVEFORM);
    rateKnob_.bindTo(synthesizer_, channel, Setting.TREMOLO_RATE);
    depthKnob_.bindTo(synthesizer_, channel, Setting.TREMOLO_DEPTH);
    attackKnob_.bindTo(synthesizer_, channel, Setting.TREMOLO_ATTACK);
    decayKnob_.bindTo(synthesizer_, channel, Setting.TREMOLO_DECAY);
    sustainKnob_.bindTo(synthesizer_, channel, Setting.TREMOLO_SUSTAIN);
    releaseKnob_.bindTo(synthesizer_, channel, Setting.TREMOLO_RELEASE);
  }

  private PianoView piano_;
  private WaveformRowView waveformView_;
  private KnobView rateKnob_;
  private KnobView depthKnob_;
  private KnobView attackKnob_;
  private KnobView decayKnob_;
  private KnobView sustainKnob_;
  private KnobView releaseKnob_;
}
