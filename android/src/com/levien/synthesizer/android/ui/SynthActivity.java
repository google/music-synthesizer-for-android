/*
 * Copyright 2013 Google Inc.
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

import com.levien.synthesizer.android.service.SynthesizerService;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;

/**
 * A base class that handles making a synthesizer connection. All activities should subclass from
 * this if sound should keep going during the activity.
 */
public class SynthActivity extends Activity {
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

  protected void onSynthConnected() {
  }

  protected void onSynthDisconnected() {
  }

  private ServiceConnection synthesizerConnection_ = new ServiceConnection() {
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    public void onServiceConnected(ComponentName className, IBinder service) {
      SynthesizerService.LocalBinder binder = (SynthesizerService.LocalBinder)service;
      synthesizerService_ = binder.getService();
      onSynthConnected();
    }
    public void onServiceDisconnected(ComponentName className) {
      onSynthDisconnected();
      synthesizerService_ = null;
    }
  };

  protected SynthesizerService synthesizerService_;
}
