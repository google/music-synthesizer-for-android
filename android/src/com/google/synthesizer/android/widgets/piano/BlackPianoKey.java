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

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;

import com.google.synthesizer.core.music.Note;

/**
 * One of the black (non-natural) keys on the piano.
 */
public class BlackPianoKey extends NotePianoKey {
  /**
   * Creates a new key.
   * @param piano - the piano this key is on.
   * @param octaveOffset - octave of the key, relative to the leftmost octave of the piano.
   * @param key - offset of the key from the start of the octave.
   */
  public BlackPianoKey(PianoView piano, int octave, int key) {
    super(piano, octave, key);
  }

  /**
   * Sets rect_ to the position of this key, based on the drawing rect of the piano it's on.
   * @param drawingRect - the position of the piano itself.
   * @param octaves - the number of octaves visible on the piano keyboard.
   */
  public void layout(Rect drawingRect, int octaves) {
    int whiteKeyWidth = getWhiteKeyWidth(drawingRect, octaves);
    int blackKeyWidth = getBlackKeyWidth(drawingRect, octaves);
    rect_.top = 0;
    rect_.bottom = rect_.top + getBlackKeyHeight(drawingRect);
    rect_.left = ((octaveOffset_ * WHITE_KEYS.length + key_ + 2) * whiteKeyWidth) -
        (blackKeyWidth/2);
    rect_.right = rect_.left + blackKeyWidth;
  }

  /**
   * Returns the log frequency of the note of the key.
   */
  public double getLogFrequency() {
    return Note.computeLog12TET(BLACK_KEYS[key_], octaveOffset_ + piano_.getFirstOctave());
  }

  /**
   * Draws the key in the current rect_.
   */
  public void draw(Canvas canvas) {
    strokePaint_.setColor(Color.BLACK);
    if (isPressed()) {
      fillPaint_.setColor(Color.GREEN);
    } else {
      fillPaint_.setColor(Color.BLACK);
    }
    canvas.drawRect(rect_, fillPaint_);
    canvas.drawRect(rect_, strokePaint_);
  }

  /**
   * Returns true if this is one of the black key positions that should actually have a key.
   */
  public static boolean isValid(int note) {
    return BLACK_KEYS[note] != Note.NONE;
  }

  /**
   * Utility function to calculate the width that a standard black key on this keyboard should be.
   */
  protected static int getBlackKeyWidth(Rect drawingRect, int octaves) {
    return (getWhiteKeyWidth(drawingRect, octaves) * 2) / 3;
  }

  /**
   * Utility function to calculate the height that a standard black key on this keyboard should be.
   */
  protected static int getBlackKeyHeight(Rect drawingRect) {
    return getWhiteKeyHeight(drawingRect) / 2;
  }
}