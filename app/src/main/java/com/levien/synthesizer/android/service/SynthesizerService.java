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

package com.levien.synthesizer.android.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import com.levien.synthesizer.R;
import com.levien.synthesizer.android.AndroidGlue;
import com.levien.synthesizer.android.usb.UsbMidiDevice;
import com.levien.synthesizer.core.midi.MessageTee;
import com.levien.synthesizer.core.midi.MidiListener;

/**
 * An Android Service that plays audio from a synthesizer.
 * The Service is created when the first Activity binds to it, and destroys itself when no more
 * Activities are bound to it.
 */
public class SynthesizerService extends Service {
  // Class for local client access.
  public class LocalBinder extends Binder {
    public SynthesizerService getService() {
      return SynthesizerService.this;
    }
  }

  /**
   * Run when the Service is first created.
   */
  @Override
  @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
  public void onCreate() {
    Log.d("synth", "service onCreate");
    if (androidGlue_ == null) {
      AudioParams params = new AudioParams(44100, 64);
      // TODO: for pre-JB-MR1 devices, do some matching against known devices to
      // get best audio parameters.
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
        getJbMr1Params(params);
      }
      // Empirical testing shows better performance with small buffer size
      // than actually matching the media server's reported buffer size.
      params.bufferSize = 64;

      androidGlue_ = new AndroidGlue();
      androidGlue_.start(params.sampleRate, params.bufferSize);
      InputStream patchIs = getResources().openRawResource(R.raw.rom1a);
      byte[] patchData = new byte[4104];
      try {
        patchIs.read(patchData);
        androidGlue_.sendMidi(patchData);
        patchNames_ = new ArrayList<String>();
        for (int i = 0; i < 32; i++) {
          patchNames_.add(new String(patchData, 124 + 128 * i, 10, "ISO-8859-1"));
        }
      } catch (IOException e) {
        Log.e(getClass().getName(), "loading patches failed");
      }
    }
    midiListener_ = new MessageTee(androidGlue_);
    androidGlue_.setPlayState(true);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
      IntentFilter filter = new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED);
      registerReceiver(usbReceiver_, filter);
      scanUsbMidi();
    }
  }

  /**
   * Run when this Service is finally destroyed.
   */
  @Override
  public void onDestroy() {
    Log.d("synth", "service onDestroy");
    androidGlue_.setPlayState(false);
    setMidiInterface(null, null);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
      unregisterReceiver(usbReceiver_);
    }
  }

  /**
   * Run when an Activity binds to this Service.
   */
  @Override
  public IBinder onBind(Intent intent) {
    return binder_;
  }

  public MidiListener getMidiListener() {
    return midiListener_;
  }

  /**
   * Sends raw MIDI data to the synthesizer.
   *
   * @param buf MIDI bytes to send
   */
  public void sendRawMidi(byte[] buf) {
    androidGlue_.sendMidi(buf);
  }

  public List<String> getPatchNames() {
    return patchNames_;
  }

  public boolean connectUsbMidi(UsbDevice device) {
    usbDeviceNeedsPermission_ = null;
    if (usbDevice_ == device) {
      return device != null;
    }
    UsbInterface intf = device != null ? UsbMidiDevice.findMidiInterface(device) : null;
    boolean success = setMidiInterface(device, intf);
    usbDevice_ = success ? device : null;
    return success;
  }

  /**
   * Call to find out whether there is a device that has been scanned
   * but not connected to because of missing permission.
   *
   * @return Device that needs permission, or null if none.
   */
  public UsbDevice usbDeviceNeedsPermission() {
    return usbDeviceNeedsPermission_;
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
  private void getJbMr1Params(AudioParams params) {
      AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
    String sr = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
    String bs = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
    params.confident = true;
    params.sampleRate = Integer.parseInt(sr);
    params.bufferSize = Integer.parseInt(bs);
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
      Log.d("synth", "closing connection " + usbMidiConnection_);
      usbMidiConnection_.close();
      usbMidiConnection_ = null;
    }
    if (device != null && intf != null) {
      UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
      UsbDeviceConnection connection = usbManager.openDevice(device);
      if (connection != null) {
        if (connection.claimInterface(intf, true)) {
          usbDevice_ = device;
          usbMidiConnection_ = connection;
          usbMidiInterface_ = intf;
          usbMidiDevice_ = new UsbMidiDevice(midiListener_, usbMidiConnection_, intf);
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

  // scan for MIDI devices on the USB bus
  @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
  private void scanUsbMidi() {
    UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
    HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
    Log.i("synth", "USB device count=" + deviceList.size());
    for (UsbDevice device : deviceList.values()) {
      UsbInterface intf = UsbMidiDevice.findMidiInterface(device);
      if (intf != null) {
        if (usbManager.hasPermission(device)) {
          if (connectUsbMidi(device)) {
            break;
          }
        } else {
          usbDeviceNeedsPermission_ = device;
        }
      }
    }
  }

  private final BroadcastReceiver usbReceiver_ = new BroadcastReceiver() {
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    public void onReceive(Context context, Intent intent) {
      Log.d("synth", "service broadcast receiver got " + intent);
      String action = intent.getAction();
      if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
        UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        if (device.equals(usbDevice_)) {
          connectUsbMidi(null);
        }
      }
    }
  };

  /**
   * Set a MidiListener. At the moment, this listener gets all MIDI events, but
   * it might change to only get them from the USB MIDI device.
   *
   * @param target MidiListener to receive messages, or null if none
   */
  public void setMidiListener(MidiListener target) {
    midiListener_.setSecondTarget(target);
  }

  private MessageTee midiListener_;

  // Binder to use for Activities in this process.
  private final IBinder binder_ = new LocalBinder();

  private static AndroidGlue androidGlue_;

  private static List<String> patchNames_;

  // State for USB MIDI keyboard connection
  private UsbDevice usbDevice_;
  private UsbDeviceConnection usbMidiConnection_;
  private UsbMidiDevice usbMidiDevice_;
  private UsbInterface usbMidiInterface_;
  private UsbDevice usbDeviceNeedsPermission_;

}
