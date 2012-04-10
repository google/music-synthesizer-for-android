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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
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

    InputStream patchIs = getResources().openRawResource(R.raw.rom1a);
    byte[] patchData = new byte[4104];
    try {
      patchIs.read(patchData);
      androidGlue_.sendMidi(patchData);
      ArrayList<String> patchNames = new ArrayList<String>();
      for (int i = 0; i < 32; i++) {
        patchNames.add(new String(patchData, 124 + 128 * i, 10, "ISO-8859-1"));
      }
      ArrayAdapter<String> adapter = new ArrayAdapter<String>(
              this, android.R.layout.simple_spinner_item, patchNames);
      adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
      presetSpinner_.setAdapter(adapter);
    } catch (IOException e) {
      Log.e(getClass().getName(), "loading patches failed");
    }
    presetSpinner_.setOnItemSelectedListener(new OnItemSelectedListener() {
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        androidGlue_.sendMidi(new byte[] {(byte)0xc0, (byte)position});
      }
      public void onNothingSelected(AdapterView<?> parent) {
      }
    });
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
