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
import java.util.HashMap;

import android.app.Activity;
import android.content.Context;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.synthesizer.R;
import com.google.synthesizer.android.AndroidGlue;
import com.google.synthesizer.android.widgets.knob.KnobListener;
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
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN |
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    setContentView(R.layout.piano2);

    piano_ = (PianoView)findViewById(R.id.piano);
    cutoffKnob_ = (KnobView)findViewById(R.id.cutoffKnob);
    resonanceKnob_ = (KnobView)findViewById(R.id.resonanceKnob);
    presetSpinner_ = (Spinner)findViewById(R.id.presetSpinner);

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
    
    piano_.bindTo(androidGlue_);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
      tryConnectUsb();
    }
  }

  @Override
  protected void onDestroy() {
    androidGlue_.shutdown();
    super.onDestroy();
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

  private UsbEndpoint getInputEndpoint(UsbInterface usbIf) {
    int nEndpoints = usbIf.getEndpointCount();
    for (int i = 0; i < nEndpoints; i++) {
      UsbEndpoint endpoint = usbIf.getEndpoint(i);
      if (endpoint.getDirection() == UsbConstants.USB_DIR_IN &&
              endpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
        return endpoint;
      }
    }
    return null;
  }
  
  private void startUsbThread(final UsbDeviceConnection connection, final UsbEndpoint endpoint) {
    Thread thread = new Thread(new Runnable() {
      public void run() {
        byte[] buf = new byte[endpoint.getMaxPacketSize()];
        while (true) {
          int nBytes = connection.bulkTransfer(endpoint, buf, buf.length, 10000);
          for (int i = 0; i < nBytes; i += 4) {
            int codeIndexNumber = buf[i] & 0xf;
            int payloadBytes = 0;
            if (codeIndexNumber == 8 || codeIndexNumber == 9 || codeIndexNumber == 11 ||
                    codeIndexNumber == 14) {
              payloadBytes = 3;
            } else if (codeIndexNumber == 12) {
              payloadBytes = 2;
            }
            if (payloadBytes > 0) {
              byte[] newBuf = new byte[payloadBytes];
              System.arraycopy(buf, i + 1, newBuf, 0, payloadBytes);
              androidGlue_.onMessage(newBuf);
            }
          }
        }
      }
    });
    thread.start();
  }
  private void tryConnectUsb() {
    UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
    HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
    TextView label = (TextView)findViewById(R.id.volumeLabel);
    if (!deviceList.isEmpty()) {
      UsbDevice device = deviceList.values().iterator().next();
      //label.setText("ic:" + device.getInterfaceCount());
      UsbInterface usbIf = device.getInterface(1);
      UsbDeviceConnection connection = usbManager.openDevice(device);
      if (connection != null) {
        connection.claimInterface(usbIf, true);
        UsbEndpoint endpoint = getInputEndpoint(usbIf);
        //label.setText(endpoint.toString());
        startUsbThread(connection, endpoint);
      } else {
        label.setText("error opening device");
      }
    }
  }
  
  private AndroidGlue androidGlue_;
  private PianoView piano_;
  private KnobView cutoffKnob_;
  private KnobView resonanceKnob_;
  private Spinner presetSpinner_;
}
