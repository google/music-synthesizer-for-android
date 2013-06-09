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

/**
 * Activity for modifying effects like echo.
 * TODO(klimt): Add the ability to switch channels.
 */
public class EffectsActivity extends SynthesizerActivity {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.effects);

    piano_ = (PianoView)findViewById(R.id.piano);
    echoMixKnob_ = (KnobView)findViewById(R.id.echoMixKnob);
    echoDelayKnob_ = (KnobView)findViewById(R.id.echoDelayKnob);
  }

  @Override
  protected void onSynthesizerUpdate(MultiChannelSynthesizer synth) {
    int channel = getIntentChannel(this);
    piano_.bindTo(synthesizer_, channel);
    echoMixKnob_.bindTo(synthesizer_, channel, Setting.ECHO_MIX);
    echoDelayKnob_.bindTo(synthesizer_, channel, Setting.ECHO_DELAY);
  }

  private PianoView piano_;
  private KnobView echoMixKnob_;
  private KnobView echoDelayKnob_;
}
