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
import android.graphics.Path;
import android.graphics.Rect;

/**
 * A key on the piano for changing the octave up or down.
 */
public class OctavePianoKey extends PianoKey {
  /**
   * Creates the key.
   * @param piano - the piano this key is on.
   * @param delta - how the octave should change when this key is pressed.
   */
  public OctavePianoKey(PianoView piano, int delta) {
    super(piano);
    arrow_ = new Path();
    delta_ = delta;
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
    if (delta_ <= 0) {
      rect_.left = 0;
      rect_.right = rect_.left + whiteKeyWidth;
    } else {
      rect_.right = drawingRect.right;
      rect_.left = rect_.right - whiteKeyWidth;
    }
  }

  /**
   * Returns true if the current octave of the piano could be changed by delta and still be valid.
   */
  private boolean isValid() {
    return (piano_.getFirstOctave() + delta_ >= 0 &&
            piano_.getFirstOctave() + piano_.getOctaves() + delta_ <= 8);
  }

  /**
   * Draws the key in the current rect_.
   */
  public void draw(Canvas canvas) {
    strokePaint_.setColor(Color.BLACK);
    fillPaint_.setColor(Color.BLACK);
    if (isPressed() && isValid()) {
      fillPaint_.setColor(Color.GREEN);
    }
    canvas.drawRect(rect_, fillPaint_);
    canvas.drawRect(rect_, strokePaint_);

    // Draw an arrow in the direction of the delta.
    if (isValid()) {
      arrow_.reset();
      if (delta_ <= 0) {
        arrow_.moveTo(rect_.left + 2, rect_.height() / 2);
        arrow_.lineTo(rect_.right - 2, rect_.height() / 2 - 20);
        arrow_.lineTo(rect_.right - 2, rect_.height() / 2 + 20);
      } else {
        arrow_.moveTo(rect_.right - 2, rect_.height() / 2);
        arrow_.lineTo(rect_.left + 2, rect_.height() / 2 - 20);
        arrow_.lineTo(rect_.left + 2, rect_.height() / 2 + 20);
      }
      arrow_.close();
      fillPaint_.setColor(Color.WHITE);
      canvas.drawPath(arrow_, fillPaint_);
    }
  }
  
  /**
   * Called when the pressed_ state has changed.
   */
  @Override
  protected void onPressedChanged(boolean move) {
    if (isPressed() && isValid()) {
      piano_.changeOctave(delta_);
    }
  }

  // This is just used for drawing, but we don't want to pay to reallocate it every time.
  private Path arrow_;

  // How the octave should change when this key is pressed.  
  private int delta_;
}