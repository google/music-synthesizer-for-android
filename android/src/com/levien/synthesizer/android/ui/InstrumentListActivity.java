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

import java.util.ArrayList;

import com.levien.synthesizer.core.model.composite.MultiChannelSynthesizer;
import com.levien.synthesizer.android.service.SynthesizerService;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * An activity that shows the list of available presets (aka instruments), and let's the user click
 * on one of them to begin editing it.
 */
public class InstrumentListActivity extends ListActivity {
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
    this.startActivity(new Intent(null,
                                  SynthesizerActivity.makeUri(position),
                                  this,
                                  EditInstrumentActivity.class));
  }

  private ServiceConnection synthesizerConnection_ = new ServiceConnection() {
    public void onServiceConnected(ComponentName className, IBinder service) {
      MultiChannelSynthesizer synthesizer =
          ((SynthesizerService.LocalBinder)service).getSynthesizer();
      ArrayList<String> presets = new ArrayList<String>();
      synthesizer.getPresetNames(presets);
      final ArrayAdapter<String> adapter = new ArrayAdapter<String>(
          InstrumentListActivity.this,
          android.R.layout.simple_list_item_1,
          presets.toArray(new String[0]));
      InstrumentListActivity.this.runOnUiThread(new Runnable() {
        public void run() {
          InstrumentListActivity.this.setListAdapter(adapter);
          InstrumentListActivity.this.getListView().invalidate();
        }
      });
      
    }
    public void onServiceDisconnected(ComponentName className) {
    }
  };
}
