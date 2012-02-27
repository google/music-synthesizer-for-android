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

package com.google.synthesizer.android.widgets.score;

import java.util.logging.Logger;

import com.google.synthesizer.R;
import com.google.synthesizer.core.music.Note;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;

/**
 * A button to enable "play" mode of the ScoreView.  In play mode, pressing anywhere plays the note
 * for the key that's being pressed.  @see ScoreView.
 */
public class PlayTool extends ScoreViewTool {
  /**
   * Creates a new PlayTool, loading resources from the given context.
   */
  PlayTool(Context context) {
    logger_ = Logger.getLogger(getClass().getName());

    keysDown_ = new int[FINGERS];
    for (int i = 0; i < keysDown_.length; ++i) {
      keysDown_[i] = -1;
    }

    icon_ = context.getResources().getDrawable(R.drawable.play_piano);
    paint_ = new Paint();
  }

  /**
   * Draws the button on the toolbar.
   * @param canvas - The canvas to draw the button on.
   * @param score - The ScoreView that this toolbar is for.
   * @param rect - The area of the button to be drawn, including any margin.
   * @param margin - The preferred margin around the button, in screen coordinates.
   */
  @Override
  public void drawButton(Canvas canvas, ScoreView score, Rect rect, float margin) {
    if (score.getTool() == this) {
      paint_.setColor(Color.WHITE);
      paint_.setStyle(Paint.Style.FILL);
      canvas.drawRect(rect.left - margin / 2,
                      rect.top - margin / 2,
                      rect.right + margin / 2,
                      rect.bottom + margin / 2,
                      paint_);
    }

    paint_.setColor(Color.BLACK);
    paint_.setStyle(Paint.Style.FILL);
    canvas.drawRect(rect, paint_);
    icon_.setBounds(rect);
    icon_.draw(canvas);
  }

  /**
   * Called after each key is drawn, to give this tool a chance to draw over it.
   * See ScoreView.onDraw() for more information on how ScoreView is drawn.
   * @param key - The key that was drawn.
   * @param canvas - The canvas the key is drawn into.
   * @param rect - The area of the key on the canvas.
   */
  @Override
  public void afterDrawKey(int key,
                           Canvas canvas,
                           Rect rect) {
    for (int keyDown : keysDown_) {
      if (key == keyDown) {
        paint_.setColor(Color.GREEN);
        paint_.setStyle(Paint.Style.FILL);
        canvas.drawRect(rect, paint_);
      }
    }
  }

  /**
   * Called to handle touch down events.
   * Returns true iff we need to redraw.
   */
  private boolean onTouchDown(ScoreView view, int finger, int physicalX, int physicalY) {
    double note = view.getNoteAt(physicalY);
    double logFrequency = Note.computeLog12TET(((int)note) % 12, ((int)note) / 12);
    view.getSynthesizer().getChannel(view.getCurrentChannel()).setPitch(logFrequency, finger);
    view.getSynthesizer().getChannel(view.getCurrentChannel()).turnOn(true, finger);
    keysDown_[finger] = (int)note;
    return true;
  }

  /**
   * Called to handle touch move events.
   */
  private boolean onTouchMove(ScoreView view, int finger, int physicalX, int physicalY) {
    double note = view.getNoteAt(physicalY);
    double logFrequency = Note.computeLog12TET(((int)note) % 12, ((int)note) / 12);
    view.getSynthesizer().getChannel(view.getCurrentChannel()).setPitch(logFrequency, finger);
    keysDown_[finger] = (int)note;
    return true;
  }

  /**
   * Called to handle touch up events.
   */
  protected boolean onTouchUp(ScoreView view, int finger) {
    view.getSynthesizer().getChannel(view.getCurrentChannel()).turnOff(finger);
    keysDown_[finger] = -1;
    return true;
  }

  /**
   * Called when the user touches the ScoreView while this tool is selected.
   * @param view - The ScoreView that this tool is for.
   * @param event - The touch event that triggered this handler.
   * @return true iff this tool handled the touch event.
   */
  @Override
  public boolean onTouch(ScoreView view, MotionEvent event) {
    int action = event.getAction();
    int actionCode = action & MotionEvent.ACTION_MASK;
    boolean redraw = false;
    if (actionCode == MotionEvent.ACTION_DOWN) {
      int pointerId = event.getPointerId(0);
      if (pointerId < FINGERS) {
        int x = (int)event.getX();
        int y = (int)event.getY();
        redraw |= onTouchDown(view, pointerId, x, y);
      }
    } else if (actionCode == MotionEvent.ACTION_POINTER_DOWN) {
      int pointerId = action >> MotionEvent.ACTION_POINTER_ID_SHIFT;
      if (pointerId < FINGERS) {
        int pointerIndex = event.findPointerIndex(pointerId);
        if (pointerIndex >= 0) {
          int x = (int)event.getX(pointerIndex);
          int y = (int)event.getY(pointerIndex);
          redraw |= onTouchDown(view, pointerId, x, y);
        }
      }
    } else if (actionCode == MotionEvent.ACTION_MOVE) {
      for (int pointerIndex = 0; pointerIndex < event.getPointerCount(); ++pointerIndex) {
        int pointerId = event.getPointerId(pointerIndex);
        if (pointerId >= FINGERS) {
          continue;
        }
        if (pointerIndex >= 0) {
          int x = (int)event.getX(pointerIndex);
          int y = (int)event.getY(pointerIndex);
          redraw |= onTouchMove(view, pointerId, x, y);
        }
      }
    } else if (actionCode == MotionEvent.ACTION_UP) {
      int pointerId = event.getPointerId(0);
      if (pointerId < FINGERS) {
        redraw |= onTouchUp(view, pointerId);
      }
      // Clean up any other pointers that have disappeared.
      for (pointerId = 0; pointerId < FINGERS; ++pointerId) {
        boolean found = false;
        for (int pointerIndex = 0; pointerIndex < event.getPointerCount(); ++pointerIndex) {
          if (pointerId == event.getPointerId(pointerIndex)) {
            found = true;
            break;
          }
        }
        if (!found) {
          redraw |= onTouchUp(view, pointerId);
        }
      }
    } else if (actionCode == MotionEvent.ACTION_POINTER_UP) {
      int pointerId = action >> MotionEvent.ACTION_POINTER_ID_SHIFT;
      if (pointerId < FINGERS) {
        redraw |= onTouchUp(view, pointerId);
      }
      // Clean up any other pointers that have disappeared.
      for (pointerId = 0; pointerId < FINGERS; ++pointerId) {
        boolean found = false;
        for (int pointerIndex = 0; pointerIndex < event.getPointerCount(); ++pointerIndex) {
          if (pointerId == event.getPointerId(pointerIndex)) {
            found = true;
            break;
          }
        }
        if (!found) {
          redraw |= onTouchUp(view, pointerId);
        }
      }
    } else {
      return false;
    }
    if (redraw) {
      view.invalidate();
    }
    return true;    
  }

  // The piano key each finger is holding down, or -1 if a finger is not pressing any key.
  private int[] keysDown_;

  // Some objects used in drawing.  They are owned here so that they don't have to be reallocated
  // and garbage collected for every pass of drawing.
  private Paint paint_;
  private Drawable icon_;

  // The number of simultaneous fingers supported by this control.
  protected static final int FINGERS = 5;

  @SuppressWarnings("unused")
  private Logger logger_;
}
