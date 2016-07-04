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

// Class representing a USB MIDI keyboard

package com.levien.synthesizer.android.usb;

import android.annotation.TargetApi;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.os.Build;
import android.util.Log;

import com.levien.synthesizer.core.midi.MessageFromBytes;
import com.levien.synthesizer.core.midi.MidiListener;

@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
public class UsbMidiDevice {
  private final MidiListener mReceiver;
  private final UsbDeviceConnection mDeviceConnection;
  private final UsbEndpoint mEndpoint;

  private final WaiterThread mWaiterThread = new WaiterThread();

  public UsbMidiDevice(MidiListener receiver, UsbDeviceConnection connection, UsbInterface intf) {
    mReceiver = receiver;
    mDeviceConnection = connection;

    mEndpoint = getInputEndpoint(intf);
  }

  private UsbEndpoint getInputEndpoint(UsbInterface usbIf) {
    Log.d("synth", "interface:" + usbIf.toString());
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

  public void start() {
    Log.d("synth", "midi USB waiter thread starting");
    mWaiterThread.start();
  }

  public void stop() {
    synchronized (mWaiterThread) {
      mWaiterThread.mStop = true;
    }
  }

  // A helper function for clients that might want to query whether a
  // device supports MIDI
  public static UsbInterface findMidiInterface(UsbDevice device) {
    int count = device.getInterfaceCount();
    for (int i = 0; i < count; i++) {
      UsbInterface usbIf = device.getInterface(i);
      if (usbIf.getInterfaceClass() == 1 && usbIf.getInterfaceSubclass() == 3) {
        return usbIf;
      }
    }
    return null;
  }

  private class WaiterThread extends Thread {
    public boolean mStop;

    public void run() {
      byte[] buf = new byte[mEndpoint.getMaxPacketSize()];
      while (true) {
        synchronized (this) {
          if (mStop) {
            Log.d("synth", "midi USB waiter thread shutting down");
            return;
          }
        }
        // Using a timeout here is a hacky workaround to shut down the
        // thread. If we could call releaseInterface, that would cause
        // the bulkTransfer to return immediately, but that causes other
        // problems.
        final int TIMEOUT = 1000;
        int nBytes = mDeviceConnection.bulkTransfer(mEndpoint, buf, buf.length, TIMEOUT);
        if (nBytes < 0) {
          //Log.e("synth", "bulkTransfer error " + nBytes);
          //  break;
        }
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
            //Log.d("synth", "sending midi");
            MessageFromBytes.send(mReceiver, buf, i + 1, payloadBytes);
          }
        }
      }
    }
  }
}
