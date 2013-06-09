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

import java.util.ArrayList;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.levien.synthesizer.R;
import com.levien.synthesizer.core.model.composite.MultiChannelSynthesizer;
import com.levien.synthesizer.android.widgets.piano.PianoView;

// TODO(klimt): Add the ability to switch channels.

public class MainActivity extends SynthesizerActivity {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    piano_ = (PianoView)findViewById(R.id.piano);
    presetSpinner_ = (Spinner)findViewById(R.id.presetSpinner);

    final Button playButton = (Button)findViewById(R.id.playButton);
    final Button recordButton = (Button)findViewById(R.id.recordButton);

    playButton.setOnClickListener(new OnClickListener() {
      public void onClick(View view) {
        if (synthesizer_.getChannel(0).isPlaying()) {
          synthesizer_.getChannel(0).stopPlaying();
          playButton.setText(R.string.play);
          recordButton.setText(R.string.record);
        } else {
          synthesizer_.getChannel(0).startPlaying();
          playButton.setText(R.string.stop);
          recordButton.setText(R.string.record);
        }
      }
    });

    recordButton.setOnClickListener(new OnClickListener() {
      public void onClick(View view) {
        if (synthesizer_.getChannel(0).isRecording()) {
          synthesizer_.getChannel(0).stopRecording();
          playButton.setText(R.string.play);
          recordButton.setText(R.string.record);
        } else {
          synthesizer_.getChannel(0).startRecording();
          playButton.setEnabled(true);
          playButton.setText(R.string.play);
          recordButton.setText(R.string.stop);
        }
      }
    });

    presetSpinner_.setOnItemSelectedListener(new OnItemSelectedListener() {
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (synthesizer_ == null) {
          return;
        }
        String preset = presetSpinner_.getItemAtPosition(position).toString();
        if (!preset.equals("")) {
          synthesizer_.getChannel(0).setPreset(preset);
        }
      }
      public void onNothingSelected(AdapterView<?> parent) {
      }
    });
  }

  @Override
  protected void onSynthesizerUpdate(MultiChannelSynthesizer synth) {
    piano_.bindTo(synthesizer_, 0);

    ArrayList<String> presetNames = new ArrayList<String>();
    presetNames.add("");
    synthesizer_.getPresetNames(presetNames);
    ArrayAdapter<String> adapter = new ArrayAdapter<String>(
        this, android.R.layout.simple_spinner_item, presetNames);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    presetSpinner_.setAdapter(adapter);
  }

  private PianoView piano_;
  private Spinner presetSpinner_;
}
