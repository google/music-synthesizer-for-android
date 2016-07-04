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

import java.util.List;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.levien.synthesizer.R;
import com.levien.synthesizer.android.widgets.keyboard.KeyboardSpec;
import com.levien.synthesizer.android.widgets.keyboard.KeyboardView;
import com.levien.synthesizer.android.widgets.keyboard.ScrollStripView;
import com.levien.synthesizer.android.widgets.knob.KnobListener;
import com.levien.synthesizer.android.widgets.knob.KnobView;
import com.levien.synthesizer.core.midi.MidiAdapter;
import com.levien.synthesizer.core.midi.MidiListener;

/**
 * Activity for simply playing the piano.
 * This version is hacked up to send MIDI to the C++ engine. This needs to
 * be refactored to make it cleaner.
 */
public class PianoActivity2 extends SynthActivity implements OnSharedPreferenceChangeListener {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    Log.d("synth", "activity onCreate " + getIntent());
    super.onCreate(savedInstanceState);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN |
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    setContentView(R.layout.piano2);

    keyboard_ = (KeyboardView)findViewById(R.id.piano);
    ScrollStripView scrollStrip_ = (ScrollStripView)findViewById(R.id.scrollstrip);
    scrollStrip_.bindKeyboard(keyboard_);
    cutoffKnob_ = (KnobView)findViewById(R.id.cutoffKnob);
    resonanceKnob_ = (KnobView)findViewById(R.id.resonanceKnob);
    overdriveKnob_ = (KnobView)findViewById(R.id.overdriveKnob);
    presetSpinner_ = (Spinner)findViewById(R.id.presetSpinner);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
      setupUsbMidi(getIntent());
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.synth_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.settings:
        startActivity(new Intent(this, SettingsActivity.class));
        return true;
      /*
      case R.id.compose:
        startActivity(new Intent(this, ScoreActivity.class));
        return true;
        */
      default:
        return super.onOptionsItemSelected(item);
    }
  }
  @Override
  protected void onDestroy() {
    Log.d("synth", "activity onDestroy");
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
      unregisterReceiver(usbReceiver_);
    }
    super.onDestroy();
  }

  @Override
  protected void onResume() {
    super.onResume();
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    prefs.registerOnSharedPreferenceChangeListener(this);
    onSharedPreferenceChanged(prefs, "keyboard_type");
    onSharedPreferenceChanged(prefs, "vel_sens");
  }

  @Override
  protected void onPause() {
    super.onPause();
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    prefs.unregisterOnSharedPreferenceChangeListener(this);

  }

  public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
    if (key.equals("keyboard_type")) {
      String keyboardType = prefs.getString(key, "2row");
      keyboard_.setKeyboardSpec(KeyboardSpec.make(keyboardType));
    } else if (key.equals("vel_sens") || key.equals("vel_avg")) {
      float velSens = prefs.getFloat("vel_sens", 0.5f);
      float velAvg = prefs.getFloat("vel_avg", 64);
      keyboard_.setVelocitySensitivity(velSens, velAvg);
    }
  }

  @Override
  protected void onNewIntent(Intent intent) {
    Log.d("synth", "activity onNewIntent " + intent);
    connectUsbFromIntent(intent);
  }

  boolean connectUsbMidi(UsbDevice device) {
    if (synthesizerService_ != null) {
      return synthesizerService_.connectUsbMidi(device);
    }
    usbDevicePending_ = device;
    return true;
  }

  @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
  boolean connectUsbFromIntent(Intent intent) {
    if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(intent.getAction())) {
      UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
      return connectUsbMidi(device);
    } else {
      return false;
    }
  }

  @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
  void setupUsbMidi(Intent intent) {
    permissionIntent_ = PendingIntent.getBroadcast(this, 0, new Intent(
            ACTION_USB_PERMISSION), 0);
    IntentFilter filter = new IntentFilter();
    filter.addAction(ACTION_USB_PERMISSION);
    registerReceiver(usbReceiver_, filter);
    connectUsbFromIntent(intent);
  }

  private static final String ACTION_USB_PERMISSION = "com.levien.synthesizer.USB_PERSMISSION";
  BroadcastReceiver usbReceiver_ = new BroadcastReceiver() {
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      if (ACTION_USB_PERMISSION.equals(action)) {
        synchronized (this) {
          UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
          if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
            connectUsbMidi(device);
          } else {
            Log.d("synth", "permission denied for device " + device);
          }
          permissionRequestPending_ = false;
        }
      }
    }
  };

  public void sendMidiBytes(byte[] buf) {
    // TODO: in future we'll want to reflect MIDI to UI (knobs turn, keys press)
    if (synthesizerService_ != null) {
      synthesizerService_.sendRawMidi(buf);
    }
  }

  @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
  protected void onSynthConnected() {
    final MidiListener synthMidi = synthesizerService_.getMidiListener();
    //piano_.bindTo(synthMidi);
    keyboard_.setMidiListener(synthMidi);

    cutoffKnob_.setKnobListener(new KnobListener() {
      public void onKnobChanged(double newValue) {
        int value = (int)Math.round(newValue * 127);
        synthMidi.onController(0, 1, value);
      }
    });
    resonanceKnob_.setKnobListener(new KnobListener() {
      public void onKnobChanged(double newValue) {
        int value = (int)Math.round(newValue * 127);
        synthMidi.onController(0, 2, value);
      }
    });
    overdriveKnob_.setKnobListener(new KnobListener() {
      public void onKnobChanged(double newValue) {
        int value = (int)Math.round(newValue * 127);
        synthMidi.onController(0, 3, value);
      }
    });

    // Connect controller changes to knob views
    synthesizerService_.setMidiListener(new MidiAdapter() {
      public void onNoteOn(final int channel, final int note, final int velocity) {
        runOnUiThread(new Runnable() {
          public void run() {
            keyboard_.onNote(note, velocity);
          }
        });
      }

      public void onNoteOff(final int channel, final int note, final int velocity) {
        runOnUiThread(new Runnable() {
          public void run() {
            keyboard_.onNote(note, 0);
          }
        });
      }

      public void onController(final int channel, final int cc, final int value) {
        runOnUiThread(new Runnable() {
          public void run() {
            if (cc == 1) {
              cutoffKnob_.setValue(value * (1.0 / 127));
            } else if (cc == 2) {
              resonanceKnob_.setValue(value * (1.0 / 127));
            } else if (cc == 3) {
              overdriveKnob_.setValue(value * (1.0 / 127));
            }
          }
        });
      }
    });

    // Populate patch names (note: we could update an existing list rather than
    // creating a new adapter, but it probably wouldn't save all that much).
    if (presetSpinner_.getAdapter() == null) {
      // Only set it once, which is a workaround that allows the preset
      // selection to persist for onCreate lifetime. Of course, it should
      // be persisted for real, instead.
      List<String> patchNames = synthesizerService_.getPatchNames();
      ArrayAdapter<String> adapter = new ArrayAdapter<String>(
              PianoActivity2.this, android.R.layout.simple_spinner_item, patchNames);
      adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
      presetSpinner_.setAdapter(adapter);
    }

    presetSpinner_.setOnItemSelectedListener(new OnItemSelectedListener() {
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        synthesizerService_.getMidiListener().onProgramChange(0, position);
      }
      public void onNothingSelected(AdapterView<?> parent) {
      }
    });

    // Handle any pending USB device events
    if (usbDevicePending_ != null) {
      synthesizerService_.connectUsbMidi(usbDevicePending_);
      usbDevicePending_ = null;
    } else {
      UsbDevice device = synthesizerService_.usbDeviceNeedsPermission();
      if (device != null) {
        synchronized (usbReceiver_) {
          if (!permissionRequestPending_) {
            permissionRequestPending_ = true;
            UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
            usbManager.requestPermission(device, permissionIntent_);
          }
        }
      }
    }
  }

  protected void onSynthDisconnected() {
    synthesizerService_.setMidiListener(null);
  }

  //private PianoView piano_;
  private KeyboardView keyboard_;
  private KnobView cutoffKnob_;
  private KnobView resonanceKnob_;
  private KnobView overdriveKnob_;
  private Spinner presetSpinner_;
  private PendingIntent permissionIntent_;
  private boolean permissionRequestPending_;
  private UsbDevice usbDevicePending_;
}
