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

import com.levien.synthesizer.core.midi.MessageInputProcessor;
import com.levien.synthesizer.core.model.composite.MultiChannelSynthesizer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Transmitter;

/**
 * MidiDeviceBinder manages the connection between a MidiDevice and its associated BindingSet.
 */
public class MidiDeviceBinder {
  /**
   * MidiDeviceBinder manages the connection between a MidiDevice and its associated BindingSet.
   * @param device - The device to connect the BindingSet to.
   * @param synth - The synthesizer to bind to.
   */
  public MidiDeviceBinder(MidiDevice device, MultiChannelSynthesizer synth) {
    device_ = device;
    synth_ = synth;
    midiIn_ = null;
    logger_ = Logger.getLogger(getClass().getName());
    try {
      if (midiIn_ == null && device_.getMaxTransmitters() != 0) {
        if (!device_.isOpen()) {
          device_.open();
        }
        midiIn_ = device_.getTransmitter();
        midiIn_.setReceiver(new Receiver() {
          public void send(MidiMessage message, long time) {
            ByteArrayInputStream stream = new ByteArrayInputStream(
                message.getMessage(), 0, message.getLength());
            try {
              MessageInputProcessor.process(stream, 0, synth_);
            } catch (IOException e) {
              logger_.log(Level.SEVERE, "Error processing Midi message.", e);
            }
          }
          public void close() {
            logger_.info("Closing MIDI receiver.");
          }
        });
      }
      if (midiOut_ == null && device_.getMaxReceivers() != 0) {
        if (!device_.isOpen()) {
          device_.open();
        }
        midiOut_ = device_.getReceiver();
      }
    } catch (MidiUnavailableException mue) {
      logger_.severe("Unable to open MIDI device.");
      midiIn_ = null;
      midiOut_ = null;
    }
  }

  /**
   * Closes the Midi device.
   */
  public void stop() {
    logger_.info("Closing MIDI device.");
    if (device_ != null) {
      device_.close();
    }
    if (midiIn_ != null) {
      midiIn_.close();
      midiIn_ = null;
    }
    if (midiOut_ != null) {
      midiOut_.close();
      midiOut_ = null;
    }
  }

  /**
   * Sends the given message to the Midi device.
   */
  public void send(MidiMessage message) {
    if (midiOut_ != null) {
      midiOut_.send(message, System.currentTimeMillis());
    }
  }

  // The synthesizer to bind to.
  private MultiChannelSynthesizer synth_;

  // The Midi device to bind to.
  private MidiDevice device_;
  private Transmitter midiIn_;
  private Receiver midiOut_;

  private Logger logger_;
}
