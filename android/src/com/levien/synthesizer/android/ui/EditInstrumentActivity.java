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

package com.levien.synthesizer.android.ui;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.levien.synthesizer.R;
import com.levien.synthesizer.core.model.composite.Presets.Setting;
import com.levien.synthesizer.android.service.SynthesizerService;

/**
 * An Activity to let the user choose a subset of a presets settings in order to edit them.
 */
public class EditInstrumentActivity extends ListActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState){
    super.onCreate(savedInstanceState);
    String[] sections = getResources().getStringArray(R.array.sections);
    ArrayAdapter<String> adapter = new ArrayAdapter<String>(
        this, android.R.layout.simple_list_item_1, sections);
    setListAdapter(adapter);
  }

  @Override
  protected void onStart() {
    super.onStart();
    bindService(new Intent(this, SynthesizerService.class),
        synthesizerConnection_, Context.BIND_AUTO_CREATE);
  }

  @Override
  protected void onStop() {
    super.onStop();
    unbindService(synthesizerConnection_);
  }

  @Override
  protected void onListItemClick(ListView list, View view, int position, long id) {
    int channel = SynthesizerActivity.getIntentChannel(this);
    switch (position) {
      case 0:
        this.startActivity(new Intent(null,
                                      SynthesizerActivity.makeUri(channel),
                                      this,
                                      VibratoActivity.class));
        break;
      case 1:
        this.startActivity(new Intent(null, SynthesizerActivity.makeUri(
            channel,
            Setting.OSCILLATOR_1_WAVEFORM,
            Setting.OSCILLATOR_1_GLIDE,
            Setting.OSCILLATOR_1_COARSE,
            Setting.OSCILLATOR_1_FINE,
            Setting.OSCILLATOR_1_VIBRATO,
            Setting.BALANCE),
            this, OscillatorActivity.class));
        break;
      case 2:
        this.startActivity(new Intent(null, SynthesizerActivity.makeUri(
            channel,
            Setting.OSCILLATOR_2_WAVEFORM,
            Setting.OSCILLATOR_2_GLIDE,
            Setting.OSCILLATOR_2_COARSE,
            Setting.OSCILLATOR_2_FINE,
            Setting.OSCILLATOR_2_VIBRATO,
            Setting.BALANCE),
            this, OscillatorActivity.class));
        break;
      case 3:
        this.startActivity(new Intent(null, SynthesizerActivity.makeUri(
            channel,
            Setting.OSCILLATOR_1_BLEND,
            Setting.OSCILLATOR_1_STRETCH,
            Setting.OSCILLATOR_1_EXCITEMENT),
            this, KarplusStrongActivity.class));
        break;
      case 4:
        this.startActivity(new Intent(null, SynthesizerActivity.makeUri(
            channel,
            Setting.OSCILLATOR_2_BLEND,
            Setting.OSCILLATOR_2_STRETCH,
            Setting.OSCILLATOR_2_EXCITEMENT),
            this, KarplusStrongActivity.class));
        break;
      case 5:
        this.startActivity(new Intent(null,
                                      SynthesizerActivity.makeUri(channel),
                                      this,
                                      TremoloActivity.class));
        break;
      case 6:
        this.startActivity(new Intent(null,
                                      SynthesizerActivity.makeUri(channel),
                                      this,
                                      LowPassFilterActivity.class));
        break;
      case 7:
        this.startActivity(new Intent(null,
                                      SynthesizerActivity.makeUri(channel),
                                      this,
                                      AmplificationActivity.class));
        break;
      case 8:
        this.startActivity(new Intent(null,
                                      SynthesizerActivity.makeUri(channel),
                                      this,
                                      EffectsActivity.class));
        break;
    }
  }

  private ServiceConnection synthesizerConnection_ = new ServiceConnection() {
    public void onServiceConnected(ComponentName className, IBinder service) {
    }
    public void onServiceDisconnected(ComponentName className) {
    }
  };
}
