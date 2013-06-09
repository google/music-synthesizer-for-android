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

package com.levien.synthesizer.android.widgets.score;

import java.util.logging.Logger;

import com.levien.synthesizer.R;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;

/**
 * A tool for adjusting the visible logical area of a ScoreView.
 * When this tool is selected, touching moves around the viewport, and pinching zooms in/out.
 */
public class ViewportTool extends ScoreViewTool {
  /**
   * Creates a new ViewportTool, loading resources from the given context.
   */
  ViewportTool(Context context) {
    logger_ = Logger.getLogger(getClass().getName());

    startX1_ = 0;
    startX2_ = 0;
    startY1_ = 0;
    startY2_ = 0;

    icon_ = context.getResources().getDrawable(R.drawable.zoom);
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
    double timeZoom = view.getTimeZoom();
    double timeOffset = view.getTimeOffset();
    double noteZoom = view.getNoteZoom();
    double noteOffset = view.getNoteOffset();

    if (actionCode == MotionEvent.ACTION_DOWN) {
      int pointerId = event.getPointerId(0);
      startX1_ = (int)event.getX();
      startY1_ = (int)event.getY();
      if (pointerId != 0) {
        logger_.severe("Initial pointer has id " + pointerId);
      }
    } else if (actionCode == MotionEvent.ACTION_POINTER_DOWN) {
      int pointerId = action >> MotionEvent.ACTION_POINTER_ID_SHIFT;
      int pointerIndex = event.findPointerIndex(pointerId);
      if (pointerId == 1 && pointerIndex >= 0) {
        startX2_ = (int)event.getX(pointerIndex);
        startY2_ = (int)event.getY(pointerIndex);
      }
    } else if (actionCode == MotionEvent.ACTION_MOVE) {
      double currentX1 = 0;
      double currentX2 = 0;
      double currentY1 = 0;
      double currentY2 = 0;
      boolean has1 = false;
      boolean has2 = false;

      // Find the current positions of the fingers.
      for (int pointerIndex = 0; pointerIndex < event.getPointerCount(); ++pointerIndex) {
        int pointerId = event.getPointerId(pointerIndex);
        if (pointerId >= 0) {
          int x = (int)event.getX(pointerIndex);
          int y = (int)event.getY(pointerIndex);
          if (pointerId == 0) {
            currentX1 = x;
            currentY1 = y;
            has1 = true;
          } else if (pointerId == 1) {
            currentX2 = x;
            currentY2 = y;
            has2 = true;
          }
        }
      }

      if (has1 && has2) {
        // Enforce that finger 1 is to the left of and above finger 2.
        if (currentX2 < currentX1) {
          double temp = currentX1;
          currentX1 = currentX2;
          currentX2 = temp;
        }
        if (currentY2 < currentY1) {
          double temp = currentY1;
          currentY1 = currentY2;
          currentY2 = temp;
        }
        if (startX2_ < startX1_) {
          double temp = startX1_;
          startX1_ = startX2_;
          startX2_ = temp;
        }
        if (startY2_ < startY1_) {
          double temp = startY1_;
          startY1_ = startY2_;
          startY2_ = temp;
        }

        // Make sure the fingers aren't too close together.
        if (startX2_ - startX1_ < 50.0) {
          startX2_ = startX1_ + 50.0;
        }
        if (currentX2 - currentX1 < 50.0) {
          currentX2 = currentX1 + 50.0;
        }
        if (startY2_ - startY1_ < 50.0) {
          startY2_ = startY1_ + 50.0;
        }
        if (currentY2 - currentY1 < 50.0) {
          currentY2 = currentY1 + 50.0;
        }

        // Figure out the parameters of the new viewport.
        double scaleXFactor = Math.abs((currentX1 - currentX2) / (startX1_ - startX2_));
        timeOffset += (startX1_ - currentX1 / scaleXFactor) /
                      (timeZoom * view.getDrawingRect().width());
        timeZoom *= scaleXFactor;

        double scaleYFactor = Math.abs((currentY1 - currentY2) / (startY1_ - startY2_));
        noteOffset -= (startY1_ - currentY1/scaleYFactor -
                       view.getDrawingRect().bottom * (1 - 1/scaleYFactor)) /
                      (noteZoom * view.getDrawingRect().height());

        noteZoom *= scaleYFactor;
        
        // Update the tracking.
        startX1_ = currentX1;
        startX2_ = currentX2;
        startY1_ = currentY1;
        startY2_ = currentY2;
        redraw = true;
      } else if (has1) {
        // Move the viewport.
        timeOffset += (startX1_ - currentX1) / (timeZoom * view.getDrawingRect().width());
        startX1_ = currentX1;
        noteOffset -= (startY1_ - currentY1) / (noteZoom * view.getDrawingRect().height());
        startY1_ = currentY1;
        redraw = true;
      }
    } else if (actionCode == MotionEvent.ACTION_UP) {
      // Snap back so we aren't showing much margin.
      // This code is commented out because it's actually much more intuitive if it doesn't snap
      // back but just shows some margin.
      /*
      if (scaleX < 1.0 / maxX) {
        offsetX = 0.0;
        scaleX = 1.0 / maxX;
        redraw = true;
      }
      if (scaleY < 1.0 / maxY) {
        offsetY = 0.0;
        scaleY = 1.0 / maxY;
        redraw = true;
      }
      if (offsetX < 0.0) {
        offsetX = 0.0;
        redraw = true;
      }
      if (offsetY < 0.0) {
        offsetY = 0.0;
        redraw = true;
      }
      if (offsetX > maxX - 1.0 / scaleX) {
        offsetX = maxX - 1.0 / scaleX;
        redraw = true;
      }
      if (offsetY > maxY - 1.0 / scaleY) {
        offsetY = maxY - 1.0 / scaleY;
        redraw = true;
      }
      */
    } else if (actionCode == MotionEvent.ACTION_POINTER_UP) {
    } else {
      return view.onTouchEvent(event);
    }

    view.setTimeZoom(timeZoom);
    view.setTimeOffset(timeOffset);
    view.setNoteZoom(noteZoom);
    view.setNoteOffset(noteOffset);

    if (redraw) {
      view.invalidate();
    }

    return true;
  }

  // The screen coordinates of the first and second fingers pressed on the ScoreView.
  // These are updated every time the viewport is updated.
  private double startX1_;
  private double startX2_;
  private double startY1_;
  private double startY2_;

  // Some objects used in drawing.  They are owned here so that they don't have to be reallocated
  // and garbage collected for every pass of drawing.
  private Paint paint_;
  private Drawable icon_;

  private Logger logger_;
}
