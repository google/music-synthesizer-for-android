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

package com.google.synthesizer.android.widgets.piano;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

import com.google.synthesizer.core.music.Note;

/**
 * PianoKey is the abstract base class for any key on the piano.
 * It keeps track of whether the key is currently being pressed.
 */
public abstract class PianoKey {
  public PianoKey(PianoView piano) {
    piano_ = piano;
    pressed_ = new boolean[PianoView.FINGERS];
    rect_ = new Rect();

    for (int i = 0; i < pressed_.length; ++i) {
      pressed_[i] = false;
    }

    // Set up some default objects for the key to draw itself with.
    fillPaint_ = new Paint();
    strokePaint_ = new Paint();
    fillPaint_.setStyle(Paint.Style.FILL);
    strokePaint_.setStyle(Paint.Style.STROKE);
    strokePaint_.setColor(Color.BLACK);
  }

  /**
   * Sets rect_ to the position of this key, based on the drawing rect of the piano it's on.
   * @param drawingRect - the position of the piano itself.
   * @param octaves - the number of octaves visible on the piano keyboard.
   */
  abstract public void layout(Rect drawingRect, int octaves);

  /**
   * Draws the key in the current rect_.
   */
  abstract public void draw(Canvas canvas);

  /**
   * Called when the key's pressed_ state has changed.
   * @param move - true if the key became pressed because the touch moved onto it.
   */
  abstract protected void onPressedChanged(boolean move);

  /**
   * Returns true if the given co-ordinate is inside the key's current rect_.
   */
  public boolean contains(int x_, int y_) {
    return rect_.contains(x_, y_);
  }

  /**
   * Returns true if any finger is pressing this key.
   */
  public boolean isPressed() {
    for (int i = 0; i < pressed_.length; ++i) {
      if (pressed_[i]) {
        return true;
      }
    }
    return false;
  }

  /**
   * Called when a finger has touched down onto this key.
   * Returns true iff whether the pressed state changed.
   */
  final public boolean onTouchDown(int finger) {
    if (finger >= pressed_.length) {
      Log.e(getClass().getName(),
            "Finger " + finger + " was pressed down, but PianoKey only supports " +
            pressed_.length + " fingers.");
    }
    boolean wasPressed = isPressed();
    pressed_[finger] = true;
    if (!wasPressed) {
      onPressedChanged(false);
      return true;
    }
    return false;
  }

  /**
   * Called on a touch event where this key is not being touched.  It may already be up.
   * Returns true iff whether the pressed state changed.
   */
  final public boolean onTouchUp(int finger) {
    if (finger >= pressed_.length) {
      Log.e(getClass().getName(),
            "Finger " + finger + " was released, but PianoKey only supports " +
            pressed_.length + " fingers.");
    }
    boolean wasPressed = isPressed();
    pressed_[finger] = false;
    boolean isPressed = isPressed();
    if (wasPressed && !isPressed) {
      onPressedChanged(false);
      return true;
    }
    return false;
  }

  /**
   * Called when there's a touch event where the finger was moved onto this key.
   * Returns true iff whether the pressed state changed.
   */
  final public boolean onTouchMoved(int finger) {
    if (finger >= pressed_.length) {
      Log.e(getClass().getName(),
            "Finger " + finger + " was pressed down, but PianoKey only supports " +
            pressed_.length + " fingers.");
    }
    boolean wasPressed = isPressed();
    pressed_[finger] = true;
    if (!wasPressed) {
      onPressedChanged(true);
      return true;
    }
    return false;
  }

  /**
   * Utility function to calculate the width that a standard white key on this keyboard should be.
   */
  protected static int getWhiteKeyWidth(Rect drawingRect, int octaves) {
    // It's +2 to reserve space for the octave-up/down buttons.
    return drawingRect.width() / ((WHITE_KEYS.length * octaves) + 2);
  }
  
  /**
   * Utility function to calculate the height that a standard white key on this keyboard should be.
   */
  protected static int getWhiteKeyHeight(Rect drawingRect) {
    return drawingRect.height();
  }

  // The piano this key is on.
  protected PianoView piano_;

  // Is each keys currently being pressed?
  protected boolean[] pressed_;

  // The area this key occupies.
  protected Rect rect_;

  // Objects for subclasses to use for painting, just so they don't have to reallocate every time.
  protected Paint fillPaint_;
  protected Paint strokePaint_;

  // Constants to map notes onto the keys.
  protected static final int WHITE_KEYS[] = {
      Note.C, Note.D, Note.E, Note.F, Note.G, Note.A, Note.B };
  protected static final int BLACK_KEYS[] = {
      Note.C_SHARP, Note.E_FLAT, Note.NONE, Note.F_SHARP, Note.A_FLAT, Note.B_FLAT, Note.NONE };
}