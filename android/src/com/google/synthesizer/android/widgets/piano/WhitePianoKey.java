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
 * A white key on the piano.
 */
public class WhitePianoKey extends NotePianoKey {
  /**
   * Creates a new key.
   * @param piano - the piano this key is on.
   * @param octaveOffset - octave of the key, relative to the leftmost octave of the piano.
   * @param key - offset of the key from the start of the octave.
   */
  public WhitePianoKey(PianoView piano, int octave, int key) {
    super(piano, octave, key);
  }

  /**
   * Sets rect_ to the position of this key, based on the drawing rect of the piano it's on.
   * @param drawingRect - the position of the piano itself.
   * @param octaves - the number of octaves visible on the piano keyboard.
   */
  public void layout(Rect drawingRect, int octaves) {
    int whiteKeyWidth = getWhiteKeyWidth(drawingRect, octaves);
    rect_.top = 0;
    rect_.bottom = getWhiteKeyHeight(drawingRect);
    rect_.left = ((octaveOffset_ * WHITE_KEYS.length + key_ + 1) * whiteKeyWidth);
    rect_.right = rect_.left + whiteKeyWidth;
  }

  /**
   * Returns the log frequency of the note of the key.
   */
  public double getLogFrequency() {
    return Note.computeLog12TET(WHITE_KEYS[key_], octaveOffset_ + piano_.getFirstOctave());
  }

  /**
   * Draws the key in the current rect_.
   */
  public void draw(Canvas canvas) {
    strokePaint_.setColor(Color.BLACK);
    if (isPressed()) {
      fillPaint_.setColor(Color.GREEN);
    } else {
      fillPaint_.setColor(Color.WHITE);
    }
    canvas.drawRect(rect_, fillPaint_);
    canvas.drawRect(rect_, strokePaint_);
  }
}