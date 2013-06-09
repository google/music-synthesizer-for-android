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

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.levien.synthesizer.R;
import com.levien.synthesizer.core.model.composite.MultiChannelSynthesizer;
import com.levien.synthesizer.core.model.composite.Presets.Setting;
import com.levien.synthesizer.android.service.SynthesizerService;

/**
 * A base class for any Android Activity that wants to interact with the SynthesizerService.
 */
public abstract class SynthesizerActivity extends Activity {
  /**
   * Called when the synthesizer model changes.
   */
  protected abstract void onSynthesizerUpdate(MultiChannelSynthesizer synth);

  /**
   * Creates a URI for a specific synthesizer path.
   * @param path - The absolute path of the component.
   */
  public static Uri makeUri(int channel, Setting... settings) {
    StringBuilder uri = new StringBuilder("content://com.levien.synthesizer/" + channel + "/");
    boolean first = true;
    for (Setting setting : settings) {
      if (!first) {
        uri.append(',');
      }
      uri.append(setting.getNumber());
      first = false;
    }
    return Uri.parse(uri.toString());
  }

  /**
   * Returns the path part of the URI that invoked an activity.
   */
  private static String getPath(Activity activity) {
    Intent intent = activity.getIntent();
    if (intent == null) {
      Log.e(SynthesizerActivity.class.getName(),
            "Attempted to get Intent module for SynthesizerActivity with no Intent.");
      return null;
    }
    Uri uri = intent.getData();
    if (uri == null) {
      Log.e(SynthesizerActivity.class.getName(),
            "Attempted to get Intent module for Intent with no URI: " + intent);
      return null;
    }
    String path = uri.getPath();
    if (path == null) {
      Log.e(SynthesizerActivity.class.getName(),
            "Attempted to get Intent module for URI with no path: " + uri);
      return null;
    }
    if (path.startsWith("/")) {
      path = path.substring(1);
    }
    return path;
  }

  /**
   * Gets the modules specified by the URI for the intent given to this Activity.
   * @return - The list of settings found, if any.  Otherwise, null.
   */
  public static Setting[] getIntentSettings(Activity activity) {
    Setting[] settings = null;
    String path = getPath(activity);
    // Clip off the channel, if it's there...
    if (path.indexOf('/') >= 0) {
      path = path.substring(path.indexOf('/') + 1);
    }
    String[] parts = path.split(", *");
    settings = new Setting[parts.length];
    for (int i = 0; i < parts.length; ++i) {
      try {
        int id = Integer.parseInt(parts[i]);
        settings[i] = Setting.valueOf(id);
      } catch (NumberFormatException e) {
        Log.e(SynthesizerActivity.class.getName(),
              "Unable to convert number \"" + parts[i] + "\" in path: " + path);
      }
    }
    return settings;
  }

  /**
   * Gets the channel specified by the URI for the intent given to this Activity.
   * @return - The channel in the intent, or 0 if none was found.
   */
  public static int getIntentChannel(Activity activity) {
    String path = getPath(activity);
    int firstSlash = path.indexOf('/');
    if (firstSlash < 0) {
      Log.e(SynthesizerActivity.class.getName(),
            "Unable to find channel number in path: " + path);
      return 0;
    }
    String channelString = path.substring(0, firstSlash);
    try {
      return Integer.parseInt(channelString);
    } catch (NumberFormatException e) {
      Log.e(SynthesizerActivity.class.getName(),
            "Unable to convert channel number \"" + channelString + "\" in path: " + path);
      return 0;
    }
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
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.options_menu, menu);
    return true;
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.piano:
        this.startActivity(new Intent(this, PianoActivity.class));
        return true;
      case R.id.chord_grid:
        this.startActivity(new Intent(this, ChordGridActivity.class));
        return true;
      case R.id.edit_instrument:
        this.startActivity(new Intent(this, InstrumentListActivity.class));
        return true;
      case R.id.compose:
        this.startActivity(new Intent(this, ScoreActivity.class));
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  private ServiceConnection synthesizerConnection_ = new ServiceConnection() {
    public void onServiceConnected(ComponentName className, IBinder service) {
      // synthesizer_ = ISynthesizerService.Stub.asInterface(service);
      synthesizer_ = ((SynthesizerService.LocalBinder)service).getSynthesizer();
      SynthesizerActivity.this.onSynthesizerUpdate(synthesizer_);
    }
    public void onServiceDisconnected(ComponentName className) {
      synthesizer_ = null;
      SynthesizerActivity.this.onSynthesizerUpdate(synthesizer_);
    }
  };

  protected MultiChannelSynthesizer synthesizer_ = null;
}
