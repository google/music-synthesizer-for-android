/*
 * Copyright 2012 Google Inc.
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

import android.app.Activity;
import android.os.Bundle;
import android.widget.Spinner;

import com.google.synthesizer.R;
import com.google.synthesizer.android.AndroidGlue;
import com.google.synthesizer.android.widgets.knob.KnobView;
import com.google.synthesizer.android.widgets.piano.PianoView;

/**
 * Activity for simply playing the piano.
 * This version is hacked up to send MIDI to the C++ engine. This needs to
 * be refactored to make it cleaner.
 */
public class PianoActivity2 extends Activity {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.piano);

    piano_ = (PianoView)findViewById(R.id.piano);
    volumeKnob_ = (KnobView)findViewById(R.id.volumeKnob);
    presetSpinner_ = (Spinner)findViewById(R.id.presetSpinner);
    // TODO: wire these up (preset spinner should send patch selection)

    androidGlue_ = new AndroidGlue();
    androidGlue_.start();

    piano_.bindTo(androidGlue_);
  }

  @Override
  protected void onPause() {
    androidGlue_.setPlayState(false);
    super.onPause();
  }

  @Override
  protected void onResume() {
    androidGlue_.setPlayState(true);
    super.onResume();
  }

  private AndroidGlue androidGlue_;
  private PianoView piano_;
  private KnobView volumeKnob_;
  private Spinner presetSpinner_;
}
