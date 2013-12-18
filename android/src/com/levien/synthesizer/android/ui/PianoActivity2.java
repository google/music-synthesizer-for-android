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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.levien.synthesizer.R;
import com.levien.synthesizer.android.AndroidGlue;
import com.levien.synthesizer.android.stats.JitterStats;
import com.levien.synthesizer.android.usb.UsbMidiDevice;
import com.levien.synthesizer.android.widgets.knob.KnobListener;
import com.levien.synthesizer.android.widgets.knob.KnobView;
import com.levien.synthesizer.android.widgets.piano.PianoView;

/**
 * Activity for simply playing the piano.
 * This version is hacked up to send MIDI to the C++ engine. This needs to
 * be refactored to make it cleaner.
 */
public class PianoActivity2 extends Activity {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    Log.d("synth", "activity onCreate");
    super.onCreate(savedInstanceState);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN |
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    setContentView(R.layout.piano2);

    piano_ = (PianoView)findViewById(R.id.piano);
    cutoffKnob_ = (KnobView)findViewById(R.id.cutoffKnob);
    resonanceKnob_ = (KnobView)findViewById(R.id.resonanceKnob);
    overdriveKnob_ = (KnobView)findViewById(R.id.overdriveKnob);
    presetSpinner_ = (Spinner)findViewById(R.id.presetSpinner);

    AudioParams params = new AudioParams(44100, 64);
    // TODO: for pre-JB-MR1 devices, do some matching against known devices to
    // get best audio parameters.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
      getJbMr1Params(params);
    }

    androidGlue_ = new AndroidGlue();
    androidGlue_.start(params.sampleRate, params.bufferSize);

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
    
    cutoffKnob_.setKnobListener(new KnobListener() {
      public void onKnobChanged(double newValue) {
        int value = (int)Math.round(newValue * 127);
        androidGlue_.onController(0, 1, value);
      }
    });
    resonanceKnob_.setKnobListener(new KnobListener() {
      public void onKnobChanged(double newValue) {
        int value = (int)Math.round(newValue * 127);
        androidGlue_.onController(0, 2, value);
      }
    });
    overdriveKnob_.setKnobListener(new KnobListener() {
      public void onKnobChanged(double newValue) {
        int value = (int)Math.round(newValue * 127);
        androidGlue_.onController(0, 3, value);
      }
    });
    
    piano_.bindTo(androidGlue_);
    // This is now done in onResume
    //tryConnectUsb();

    final boolean doStats = false;

    if (doStats) {
      jitterStats_ = new JitterStats();
      jitterStats_.setNominalCb(params.bufferSize / (double)params.sampleRate);
      statusHandler_ = new Handler();
      statusRunnable_ = new Runnable() {
        public void run() {
          int n = androidGlue_.statsBytesAvailable();
          if (n > 0) {
            byte[] buf = new byte[n];
            androidGlue_.readStatsBytes(buf, 0, n);
            jitterStats_.aggregate(buf);
            TextView statusTextView = (TextView)findViewById(R.id.status);
            statusTextView.setText(jitterStats_.report());
          }
          statusHandler_.postDelayed(statusRunnable_, 100);
        }
      };
      statusRunnable_.run();
    }

    // Create burst of load -- test code to be removed. Ultimately we'll
    // be able to get this kind of functionality by hooking up the sequencer
    if (false) {
      new Handler().postDelayed(new Runnable() {
        public void run() {
          int n = 110;
          byte[] midi = new byte[n * 3];
          for (int i = 0; i < n; i++) {
            midi[i * 3] = (byte)0x90;
            midi[i * 3 + 1] = (byte)(1 + i);
            midi[i * 3 + 2] = 64;
          }
          androidGlue_.sendMidi(midi);
        }
      }, 10000);
    }
    Button captureButton = (Button) findViewById(R.id.capture);
    captureButton.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        TextView stats = (TextView) findViewById(R.id.stats);
        stats.setText(jitterStats_.reportLong());
      }
    });
  }

  @Override
  protected void onDestroy() {
    Log.d("synth", "activity onDestroy");
    androidGlue_.shutdown();
    super.onDestroy();
  }
  
  @Override
  protected void onPause() {
    Log.d("synth", "activity onPause");
    androidGlue_.setPlayState(false);
    unregisterReceiver(usbReceiver_);
    setMidiInterface(null, null);
    super.onPause();
  }

  @Override
  protected void onResume() {
    Log.d("synth", "activity onResume");
    androidGlue_.setPlayState(true);
    tryConnectUsb();
    super.onResume();
  }

  @Override
  protected void onNewIntent(Intent intent) {
    Log.d("synth", "activity onNewIntent " + intent);
  }

  @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
  static private UsbInterface findMidiInterface(UsbDevice device) {
    int count = device.getInterfaceCount();
    for (int i = 0; i < count; i++) {
      UsbInterface usbIf = device.getInterface(i);
      if (usbIf.getInterfaceClass() == 1 && usbIf.getInterfaceSubclass() == 3) {
        return usbIf;
      }
    }
    return null;
  }
  
  @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
  private boolean setMidiInterface(UsbDevice device, UsbInterface intf) {
    if (usbMidiConnection_ != null) {
      if (usbMidiDevice_ != null) {
        usbMidiDevice_.stop();
        usbMidiDevice_ = null;
      }
      if (usbMidiInterface_ != null) {
        // Note: releasing the interface seems to trigger bugs, so
        // based on experimentation just closing seems more robust
        //usbMidiConnection_.releaseInterface(usbMidiInterface_);
      }
      usbMidiConnection_.close();
      usbMidiConnection_ = null;
      usbMidiDeviceName_ = null;
    }
    if (device != null && intf != null) {
      UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
      UsbDeviceConnection connection = usbManager.openDevice(device);
      if (connection != null) {
        if (connection.claimInterface(intf, true)) {
          usbMidiDeviceName_ = device.getDeviceName();
          usbMidiConnection_ = connection;
          usbMidiInterface_ = intf;
          usbMidiDevice_ = new UsbMidiDevice(this, usbMidiConnection_, intf);
          usbMidiDevice_.start();
          return true;
        } else {
          Log.e("synth", "failed to claim USB interface");
          connection.close();
        }
      } else {
        Log.e("synth", "open device failed");
      }
    }
    return false;
  }

  @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
  private void tryConnectUsb() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR1) {
      return;
    }
    UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
    HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
    Log.i("synth", "USB device count=" + deviceList.size());
    for (UsbDevice device : deviceList.values()) {
      Log.i("synth", "usb name=" + device.toString() + " #if=" + device.getInterfaceCount());
      UsbInterface usbIf = findMidiInterface(device);
      if (setMidiInterface(device, usbIf)) {
        break;
      }
    }
    IntentFilter filter = new IntentFilter();
    // Attach intent is handled in manifest, don't want to get it twice.
    //filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
    filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
    registerReceiver(usbReceiver_, filter);
  }

  BroadcastReceiver usbReceiver_ = new BroadcastReceiver() {
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
        Log.i("synth", "broadcast receiver: attach");
        UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        UsbInterface intf = findMidiInterface(device);
        if (intf != null) {
          setMidiInterface(device, intf);
        }
      } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
        Log.i("synth", "broadcast receiver: detach");
        UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        String deviceName = device.getDeviceName();
        if (usbMidiDeviceName_ != null && usbMidiDeviceName_.equals(deviceName)) {
          setMidiInterface(null, null);
        }
      }
    }
  };

  public void sendMidiBytes(byte[] buf) {
    // TODO: in future we'll want to reflect MIDI to UI (knobs turn, keys press)
    androidGlue_.sendMidi(buf);
  }

  class AudioParams {
    AudioParams(int sr, int bs) {
      confident = false;
      sampleRate = sr;
      bufferSize = bs;
    }
    public String toString() {
      return "sampleRate=" + sampleRate + " bufferSize=" + bufferSize;
    }
    boolean confident;
    int sampleRate;
    int bufferSize;
  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
  void getJbMr1Params(AudioParams params) {
      AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
    String sr = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
    String bs = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
    params.confident = true;
    params.sampleRate = Integer.parseInt(sr);
    params.bufferSize = Integer.parseInt(bs);
    //log("from platform: " + params);
  }

  private AndroidGlue androidGlue_;
  private PianoView piano_;
  private KnobView cutoffKnob_;
  private KnobView resonanceKnob_;
  private KnobView overdriveKnob_;
  private Spinner presetSpinner_;
  private Handler statusHandler_;
  private Runnable statusRunnable_;
  private JitterStats jitterStats_;
  private UsbDeviceConnection usbMidiConnection_;
  private UsbMidiDevice usbMidiDevice_;
  private UsbInterface usbMidiInterface_;
  private String usbMidiDeviceName_;
}
