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
import com.levien.synthesizer.core.music.Music.Event;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;

/**
 * A tool for creating new events in the score.
 */
public class NewEventTool extends ScoreViewTool {
  /**
   * Creates a new NewEventTool.
   * @param context - The context to use for loading resources.
   * @param eventTool - The tool to use for editing the event as it's created.
   */
  NewEventTool(Context context, EditEventTool eventTool) {
    logger_ = Logger.getLogger(getClass().getName());
    eventTool_ = eventTool;

    icon_ = context.getResources().getDrawable(R.drawable.add);
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
    if (actionCode == MotionEvent.ACTION_DOWN) {
      double time = view.getTimeAt((int)event.getX());
      double note = view.getNoteAt((int)event.getY());

      if (view.getSnapTo() != 0) {
        time = ((int)(time / view.getSnapTo())) * view.getSnapTo();
      }

      Event.Builder e = view.getScore().addEventBuilder();
      e.setStart(time);
      e.setEnd(time + view.getSnapTo());
      e.setKey((int)note);
      e.getKeyEventBuilder().setChannel(view.getCurrentChannel());

      eventTool_.pickupEvent(view, e, (int)event.getX(), (int)event.getY(), this);
      view.setTool(eventTool_);
      return true;
    } else {
      return false;
    }
  }

  // The tool that's used to edit the event after it's created, but only until the finger is up.
  // Then control returns back to this control.
  private EditEventTool eventTool_;

  // Some objects used in drawing.  They are owned here so that they don't have to be reallocated
  // and garbage collected for every pass of drawing.
  private Paint paint_;
  private Drawable icon_;

  @SuppressWarnings("unused")
  private Logger logger_;
}
