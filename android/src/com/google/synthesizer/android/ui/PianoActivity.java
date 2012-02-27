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

import java.util.ArrayList;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemSelectedListener;

import com.google.synthesizer.R;
import com.google.synthesizer.android.widgets.knob.KnobView;
import com.google.synthesizer.android.widgets.piano.PianoView;
import com.google.synthesizer.core.model.composite.MultiChannelSynthesizer;
import com.google.synthesizer.core.model.composite.Presets.Setting;

/**
 * Activity for simply playing the piano.
 */
public class PianoActivity extends SynthesizerActivity {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.piano);

    piano_ = (PianoView)findViewById(R.id.piano);
    volumeKnob_ = (KnobView)findViewById(R.id.volumeKnob);
    presetSpinner_ = (Spinner)findViewById(R.id.presetSpinner);

    presetSpinner_.setOnItemSelectedListener(new OnItemSelectedListener() {
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (synthesizer_ == null) {
          return;
        }
        if (position > 0) {
          piano_.bindTo(synthesizer_, position - 1);
          volumeKnob_.bindTo(synthesizer_, position - 1, Setting.VOLUME);
        }
      }
      public void onNothingSelected(AdapterView<?> parent) {
      }
    });
  }

  @Override
  protected void onSynthesizerUpdate(MultiChannelSynthesizer synth) {
    piano_.bindTo(synthesizer_, 0);
    volumeKnob_.bindTo(synthesizer_, 0, Setting.VOLUME);

    ArrayList<String> presetNames = new ArrayList<String>();
    presetNames.add("");
    synthesizer_.getPresetNames(presetNames);
    ArrayAdapter<String> adapter = new ArrayAdapter<String>(
        this, android.R.layout.simple_spinner_item, presetNames);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    presetSpinner_.setAdapter(adapter);
  }

  private PianoView piano_;
  private KnobView volumeKnob_;
  private Spinner presetSpinner_;
}
