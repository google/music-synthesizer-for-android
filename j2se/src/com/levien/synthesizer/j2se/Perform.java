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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.midi.MidiMessage;

import com.levien.synthesizer.core.midi.MessageOutputProcessor;
import com.levien.synthesizer.core.midi.MidiFilePlayer;
import com.levien.synthesizer.core.midi.MidiListener;
import com.levien.synthesizer.core.model.composite.MultiChannelSynthesizer;
import com.levien.synthesizer.core.soundfont.SoundFontReader;
import com.levien.synthesizer.core.wave.WaveAdapter;

/**
 * Perform is a simple little command-line app for debugging the synthesizer.
 */
public class Perform {
  /**
   * Loads presets from the given file into the synthesizer.
   * @param filename - The path to the file.
   * @param synth - The synthesizer to load the presets into.
   */
  public void loadPresetsFile(String filename, MultiChannelSynthesizer synth) {
    InputStream input;
    try {
      input = new BufferedInputStream(new FileInputStream(new File(filename)));
      try {
        synth.loadLibraryFromText(input);
      } catch (IOException e) {
        logger_.log(Level.SEVERE, "Unable to read from input stream.", e);
      }
    } catch (FileNotFoundException e) {
      logger_.severe("Unable to open file " + filename + ".");
    }
  }

  /**
   * This is dumb.
   */
  private static class DummyMidiMessage extends MidiMessage {
    protected DummyMidiMessage(byte[] data) {
      super(data);
    }

    @Override
    public Object clone() {
      return new DummyMidiMessage(getMessage());
    }
  }

  /**
   * The real main() function for this class.  Creates a synthesizer and runs a read-eval-print loop
   * to take input from the user and control the synthesizer.
   */
  public Perform() {
    logger_ = Logger.getLogger(getClass().getName());
    double sampleRateInHz = 11025;

    SoundFontReader samples = null;
    try {
      samples = new SoundFontReader(new FileInputStream(new File("android/res/raw/drums.sf2")));
    } catch (IOException e) {
      samples = null;
      e.printStackTrace();
    }

    MultiChannelSynthesizer synth = new MultiChannelSynthesizer(16, 5, sampleRateInHz, samples);
    WaveAdapter writer = new WaveAdapter((int)sampleRateInHz, 16, synth);

    // Load some arbitrary presets to make playing midis more interesting.
    loadPresetsFile("android/res/raw/presets.txt", synth);
    synth.setPreset(0, 0);
    synth.setPreset(1, 1);
    synth.setPreset(2, 3);
    synth.setPreset(3, 0);
    synth.setPreset(4, 1);
    synth.setPreset(5, 1);
    synth.setPreset(6, 0);
    synth.setPreset(7, 0);
    synth.setPreset(8, 0);
    synth.setPreset(9, 5);
    synth.setPreset(10, 2);
    synth.setPreset(11, 0);
    synth.setPreset(12, 0);
    synth.setPreset(13, 0);
    synth.setPreset(14, 0);
    synth.setPreset(15, 0);

    final Midi midi = new Midi(synth);
    List<MidiListener> listeners = new ArrayList<MidiListener>();
    listeners.add(synth);
    listeners.add(new MessageOutputProcessor() {
      @Override
      protected void onMessage(byte[] message) {
        midi.send(new DummyMidiMessage(message));
      }
    });
    MidiFilePlayer player = new MidiFilePlayer(listeners);

    SynthesizerThread thread = new SynthesizerThread(writer, (int)sampleRateInHz);
    thread.play();

    boolean running = true;
    BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
    while (running) {
      String line = null;
      System.out.print("\n[synth]$ ");
      System.out.flush();
      try {
        line = stdin.readLine();
      } catch (IOException e) {
        logger_.log(Level.SEVERE, "Error reading line.", e);
        running = false;
      }
      if (line == null) {
        running = false;
      } else if (line.trim().equals("")) {
        // Do nothing.
      } else if (line.equals("exit") || line.equals("quit")) {
        running = false;
      } else if (line.equals("ls")) {
        midi.printDevices();
      } else if (line.equals("ls midi")) {
        midi.printDevices();
      } else if (line.equals("ls presets")) {
        ArrayList<String> names = new ArrayList<String>();
        synth.getPresetNames(names);
        StringBuilder output = new StringBuilder("");
        for (String name : names) {
          output.append("\n" + name);
        }
        logger_.info(output.toString());
      } else if (line.startsWith("bind ")) {
        String bindArgs = line.substring("bind ".length());
        try {
          int index = Integer.parseInt(bindArgs);
          midi.bindDevice(index);
        } catch (NumberFormatException e) {
          logger_.severe("Invalid number format \"" + bindArgs + "\".");
        }
      } else if (line.startsWith("presets ")) {
        String filename = line.substring("presets ".length());
        loadPresetsFile(filename, synth);
      } else if (line.startsWith("play ")) {
        String filename = line.substring("play ".length());
        try {
          player.play(new FileInputStream(filename));
        } catch (FileNotFoundException e) {
          logger_.log(Level.SEVERE, "Unable to open midi file.", e);
        } catch (IOException e) {
          logger_.log(Level.SEVERE, "Unable to read midi file.", e);
        }
      } else if (line.startsWith("record ")) {
        String[] args = line.split(" +", 3);
        if (args.length != 3) {
          logger_.severe("Usage: record <time> <file>.");
        }
        try {
          double time = Double.parseDouble(args[1]);
          String filename = args[2];
          writer.startRecording(time, new FileOutputStream(new File(filename)));
        } catch (NumberFormatException e) {
          logger_.severe("Invalid number format \"" + args[1] + "\".");
        } catch (FileNotFoundException e) {
          logger_.severe("Unable to create file \"" + args[2] + "\".");
        }
      } else {
        logger_.severe("Unknown command.");
      }
    }

    writer.close();
    thread.stop();
    thread.waitForStop();
    midi.stop();
  }

  public static void main(String[] args) {
    new Perform();
  }

  private Logger logger_;
}

