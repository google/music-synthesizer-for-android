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
import com.google.synthesizer.core.model.composite.MultiChannelSynthesizer;
import com.google.synthesizer.core.model.composite.Presets.Setting;

/**
 * Activity for modifying amplification/adsr envelope.
 * TODO(klimt): Add the ability to switch channels.
 */
public class AmplificationActivity extends SynthesizerActivity {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.amplification);

    attackKnob_ = (KnobView)findViewById(R.id.attackKnob);
    decayKnob_ = (KnobView)findViewById(R.id.decayKnob);
    sustainKnob_ = (KnobView)findViewById(R.id.sustainKnob);
    releaseKnob_ = (KnobView)findViewById(R.id.releaseKnob);
    volumeKnob_ = (KnobView)findViewById(R.id.volumeKnob);
    piano_ = (PianoView)findViewById(R.id.piano);
  }

  @Override
  protected void onSynthesizerUpdate(MultiChannelSynthesizer synth) {
    int channel = getIntentChannel(this);
    attackKnob_.bindTo(synthesizer_, channel, Setting.ATTACK);
    decayKnob_.bindTo(synthesizer_, channel, Setting.DECAY);
    sustainKnob_.bindTo(synthesizer_, channel, Setting.SUSTAIN);
    releaseKnob_.bindTo(synthesizer_, channel, Setting.RELEASE);
    volumeKnob_.bindTo(synthesizer_, channel, Setting.VOLUME);
    piano_.bindTo(synthesizer_, channel);
  }

  private KnobView attackKnob_;
  private KnobView decayKnob_;
  private KnobView sustainKnob_;
  private KnobView releaseKnob_;
  private KnobView volumeKnob_;
  private PianoView piano_;
}
