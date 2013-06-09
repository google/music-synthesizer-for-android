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

package com.levien.synthesizer.android.ui;

import java.util.ArrayList;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.levien.synthesizer.R;
import com.levien.synthesizer.core.model.composite.MultiChannelSynthesizer;
import com.levien.synthesizer.android.widgets.ChordGridView;

/**
 * Activity for playing whole chords at a time, arranged in a circle of fifths.
 */
public class ChordGridActivity extends SynthesizerActivity {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.chord_grid);

    chordGrid_ = (ChordGridView)findViewById(R.id.chord_grid);
    presetSpinner_ = (Spinner)findViewById(R.id.presetSpinner);

    presetSpinner_.setOnItemSelectedListener(new OnItemSelectedListener() {
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (synthesizer_ == null) {
          return;
        }
        if (position > 0) {
          chordGrid_.bindTo(synthesizer_, position - 1);
        }
      }
      public void onNothingSelected(AdapterView<?> parent) {
      }
    });
  }

  @Override
  protected void onSynthesizerUpdate(MultiChannelSynthesizer synth) {
    chordGrid_.bindTo(synthesizer_, 0);

    ArrayList<String> presetNames = new ArrayList<String>();
    presetNames.add("");
    synthesizer_.getPresetNames(presetNames);
    ArrayAdapter<String> adapter = new ArrayAdapter<String>(
        this, android.R.layout.simple_spinner_item, presetNames);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    presetSpinner_.setAdapter(adapter);
  }

  private ChordGridView chordGrid_;
  private Spinner presetSpinner_;
}
