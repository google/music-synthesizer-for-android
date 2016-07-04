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

package com.levien.synthesizer.core.midi;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A MidiFilePlayer can play .mid or .smf files using the specified MultiChannelSynthesizer.
 */
public class MidiFilePlayer extends MidiAdapter {
  /**
   * Creates a new MidiFilePlayer and connects it to a synthesizer.
   * @param synth - The synthesizer to use for playback.
   */
  public MidiFilePlayer(List<MidiListener> listeners) {
    logger_ = Logger.getLogger(getClass().getName());
    listeners_ = listeners;
    microsecondsPerQuarterNote_ = 60000000 / 120;
  }

  /**
   * Reads a midi file from input and plays in on its associated synthesizer.
   * The function blocks and returns only when the file has finished playing.
   * @param input - The stream to read the file from.
   * @throws IOException - On any kind of read error or invalid file format.
   */
  public void play(InputStream input) throws IOException {
    microsecondsPerQuarterNote_ = 60000000 / 120;
    double bpm = 60000000.0 / microsecondsPerQuarterNote_;
    logger_.info("Setting tempo to " + bpm + " bpm.");

    MidiFile midi = new MidiFile(input);
    int ticksPerBeat = midi.getHeader().getTicksPerBeat();

    // The position of the play head in each track.
    int[] position = new int[midi.getTrackCount()];
    // The time until the next event in each track.
    long[] deltaTime = new long[midi.getTrackCount()];
    for (int i = 0; i < position.length; ++i) {
      position[i] = 0;
      deltaTime[i] = midi.getTrack(i).getEvent(position[i]).getDeltaTime();
    }

    while (true) {
      // Find the track with the next available time...
      int track = -1;
      long minDeltaTime = -1;
      for (int i = 0; i < midi.getTrackCount(); ++i) {
        if (position[i] < midi.getTrack(i).getEventCount()) {
          if (minDeltaTime == -1 || deltaTime[i] < minDeltaTime) {
            track = i;
            minDeltaTime = deltaTime[i];
          }
        }
      }
      if (track == -1) {
        return;
      }

      // Extract the next event we're going to do.
      MidiEvent event = midi.getTrack(track).getEvent(position[track]);
      if (deltaTime[track] != 0) {
        try {
          // Sleep until the next event should occur.
          double microsecondsPerTick = (double)microsecondsPerQuarterNote_ / ticksPerBeat;
          double millisecondsPerTick = microsecondsPerTick / 1000.0;
          double milliseconds = Math.round(millisecondsPerTick * deltaTime[track]);
          Thread.sleep((int)(milliseconds));
        } catch (InterruptedException e) {
          throw new RuntimeException("Interrupted during sleep.");
        }
      }

      // Move past the event, and subtract the time passed from every pending event.
      for (int i = 0; i < midi.getTrackCount(); ++i) {
        if (i != track) {
          if (position[i] < midi.getTrack(i).getEventCount()) {
            deltaTime[i] -= deltaTime[track];
          }
        }
      }
      position[track]++;
      if (position[track] < midi.getTrack(track).getEventCount()) {
        deltaTime[track] = midi.getTrack(track).getEvent(position[track]).getDeltaTime();
      } else {
        deltaTime[track] = -1;
      }

      // Process the event.
      try {
        MessageInputProcessor.process(new ByteArrayInputStream(event.getMessage()), 0, this);
      } catch (IndexOutOfBoundsException e) {
        logger_.log(Level.SEVERE, "Bad message: \n" + event.getMessage(), e);
      } catch (IOException e) {
        logger_.log(Level.SEVERE, "Bad message: \n" + event.getMessage(), e);
      }
    }
  }

  /**
   * Called on midi set-tempo events.
   */
  @Override
  public void onSetTempo(int microsecondsPerQuarterNote) {
    microsecondsPerQuarterNote_ = microsecondsPerQuarterNote;
    double bpm = 60000000.0 / microsecondsPerQuarterNote_;
    logger_.info("Changing tempo to " + bpm + " bpm.");
    for (MidiListener listener : listeners_) {
      listener.onSetTempo(microsecondsPerQuarterNote);
    }
  }

  /**
   * Called on midi note-on events.
   */
  @Override
  public void onNoteOn(int channel, int note, int velocity) {
    for (MidiListener listener : listeners_) {
      listener.onNoteOn(channel, note, velocity);
    }
  }

  /**
   * Called on midi note-off events.
   */
  @Override
  public void onNoteOff(int channel, int note, int velocity) {
    for (MidiListener listener : listeners_) {
      listener.onNoteOff(channel, note, velocity);
    }
  }
  
  // TODO(klimt):  Override all the other MidiListener methods.

  // The synthesizer to play the song.
  private List<MidiListener> listeners_;

  // The current tempo of the file.
  private int microsecondsPerQuarterNote_;

  private Logger logger_;
}
