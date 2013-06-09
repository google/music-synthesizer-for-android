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

package com.levien.synthesizer.core.model.composite;

import com.levien.synthesizer.core.model.SignalProvider;
import com.levien.synthesizer.core.music.Note;
import com.levien.synthesizer.core.soundfont.SoundFontReader;

/**
 * MidiSynthesizer is a wrapper around MultiTouchSynthesizer that allows it to accept Midi input in
 * a more natural way.  As more notes are played, more simulated fingers are used, until there
 * aren't any more available, in which case notes are dropped.
 */
public class MidiSynthesizer extends MultiTouchSynthesizer implements SignalProvider {
  /**
   * Creates a new MidiSynthesizer that wraps MultiTouchSynthesizer with a given number of fingers.
   * @param fingers - How many fingers to simulate.
   * @param sampleRateInHz - The sample rate of the wrapped synthesizer.
   */
  public MidiSynthesizer(int fingers, double sampleRateInHz, SoundFontReader sampleProvider) {
    super(fingers, sampleRateInHz, sampleProvider);
    noteDown_ = new int[FINGERS];
    for (int i = 0; i < FINGERS; ++i) {
      noteDown_[i] = -1;
    }
  }

  /**
   * Called to handle Midi note-on events.
   * @param note - The note to turn on.
   * @param velocity - How hard the key was pressed, from 0 to 127.
   */
  public void onNoteOn(int note, int velocity) {
    if (velocity == 0) {
      onNoteOff(note, velocity);
    } else {
      for (int i = 0; i < FINGERS; ++i) {
        if (noteDown_[i] < 0) {
          noteDown_[i] = note;
          setPitch(Note.computeLog12TET(note % 12, note / 12), i);
          turnOn(true, i);
          break;
        }
      }
    }
  }

  /**
   * Called to handle Midi note-off events.
   * @param note - The note to turn off.
   * @param velocity - How hard the key was (un?)pressed, from 0 to 127.
   */
  public void onNoteOff(int note, int velocity) {
    for (int i = 0; i < FINGERS; ++i) {
      if (noteDown_[i] == note) {
        noteDown_[i] = -1;
        turnOff(i);
        break;
      }
    }
  }

  // The map of which notes are being held down by each finger.
  private int[] noteDown_;
}
