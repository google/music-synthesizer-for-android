/*
 * Copyright 2011 Google Inc.
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

package com.levien.synthesizer.j2se;

import com.levien.synthesizer.core.model.composite.MultiChannelSynthesizer;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;

/**
 * Midi keeps track of the Midi devices connected to the system, and their control bindings.
 */
public class Midi {
  /**
   * Creates a new Midi system for the given synthesizer.
   */
  public Midi(MultiChannelSynthesizer synth) {
    synthesizer_ = synth;
    binders_ = new ArrayList<MidiDeviceBinder>();
    logger_ = Logger.getLogger(getClass().getName());
  }

  /**
   * Prints out a list of all of the Midi devices connected to the system.
   */
  public void printDevices() {
    StringBuilder debug = new StringBuilder("\n");
    MidiDevice.Info[] devices = MidiSystem.getMidiDeviceInfo();
    for (int i = 0; i < devices.length; ++i) {
      debug.append("Device " + i + ": " +
                   devices[i].getName() + ": " + devices[i].getDescription() + "\n");
      debug.append("  " + devices[i].getVendor());
      debug.append("  " + devices[i].getVersion());
      try {
        MidiDevice device = MidiSystem.getMidiDevice(devices[i]);
        debug.append("  receivers: " + device.getMaxReceivers() +
                     "  transmitters: " + device.getMaxTransmitters() + "\n");
      } catch (MidiUnavailableException mue) {
        debug.append("  Unavailable!");
      }
    }
    logger_.info(debug.toString());
  }

  /**
   * Returns the Midi device at the specified index.
   */
  private MidiDevice getDevice(int index) {
    MidiDevice.Info[] devices = MidiSystem.getMidiDeviceInfo();
    if (index < 0 || index >= devices.length) {
      return null;
    }
    try {
      return MidiSystem.getMidiDevice(devices[index]);
    } catch (MidiUnavailableException mue) {
      return null;
    }
  }

  /**
   * Connects the synthesizer to the controls of the Midi device at the specified index.
   */
  public void bindDevice(int index) {
    MidiDevice device = getDevice(index);
    if (device != null) {
      binders_.add(new MidiDeviceBinder(device, synthesizer_));
    } else {
      logger_.severe("Unable to get device with index " + index + ".");
    }
  }

  /**
   * Stops the connection to all Midi devices.
   */
  public void stop() {
    for (MidiDeviceBinder binder : binders_) {
      binder.stop();
    }
  }

  /**
   * Sends the given message to all Midi devices.
   */
  public void send(MidiMessage message) {
    for (MidiDeviceBinder binder : binders_) {
      binder.send(message);
    }
  }

  // The synthesizer to bind controls to.
  private MultiChannelSynthesizer synthesizer_;

  // The list of Midi device bindings.
  private List<MidiDeviceBinder> binders_;

  private Logger logger_;
}
