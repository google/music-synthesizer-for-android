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

package com.google.synthesizer.android.widgets.piano;


/**
 * Abstract base class for keys on the piano that play a note when pressed.
 */
public abstract class NotePianoKey extends PianoKey {
  /**
   * Creates a new key.
   * @param piano - the piano this key is on.
   * @param octaveOffset - octave of the key, relative to the leftmost octave of the piano.
   * @param key - offset of the key from the start of the octave.
   */
  public NotePianoKey(PianoView piano, int octaveOffset, int key) {
    super(piano);
    octaveOffset_ = octaveOffset;
    key_ = key;
  }

  /**
   * Called when the pressed_ state has changed.
   */
  protected void onPressedChanged(boolean move) {
  }

  /**
   * Returns the log frequency of the note of the key.
   */
  abstract protected double getLogFrequency();

  // Octave of the key, relative to the leftmost octave of the piano.
  protected int octaveOffset_;

  // Offset of the key from the start of the octave.
  protected int key_;
}