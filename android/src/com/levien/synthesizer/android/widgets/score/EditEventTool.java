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
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;

/**
 * A tool for editing the events in a score.  If the user touches an event, they can move it or
 * resize it.  If the event is already selected, all selected events will be moved or resized in the
 * same way.  If the user touches outside any events, a selection rectangle allows them to select
 * one or more events.
 */
public class EditEventTool extends ScoreViewTool {
  /**
   * Creates a new EditEventTool with the given context for loading resources.
   */
  EditEventTool(Context context) {
    logger_ = Logger.getLogger(getClass().getName());

    // Set up basic drawing structs, just so we don't have to allocate this later when we draw.
    paint_ = new Paint();
    path_ = new Path();

    selection_ = new Rect();

    trashVisible_ = false;
    trashRect_ = new Rect();
    trashIcon_ = context.getResources().getDrawable(R.drawable.trash);

    icon_ = context.getResources().getDrawable(R.drawable.arrow);
  }

  /**
   * Starts this tool editing a particular event, and sets it to invoke another tool when done.
   * Called by NewEventTool to handle sizing the tool as it's being created.
   * @param view - The ScoreView for this tool.
   * @param event - The event to start editing.
   * @param physicalX - The current x of the user's finger, in screen coordinates.
   * @param physicalY -The current y of the user's finger, in screen coordinates.
   * @param nextTool - The tool to select when the user is done editing this event.  (on touch up)
   */
  public void pickupEvent(ScoreView view,
                          Event.Builder event,
                          int physicalX,
                          int physicalY,
                          ScoreViewTool nextTool) {
    // Save the tool to bring up after this operation is complete.
    nextTool_ = nextTool;

    // De-select everything.
    for (int j = 0; j < view.getScore().getEventCount(); ++j) {
      view.getScore().getEventBuilder(j).setSelected(false);
    }
    // Select just the one that was picked up.
    event.setSelected(true);
    mode_ = RESIZING_RIGHT;

    // Make sure to snap the event.
    if (!event.hasUnsnappedStart()) {
      event.setUnsnappedStart(event.getStart());
    }
    if (!event.hasUnsnappedEnd()) {
      event.setUnsnappedEnd(event.getEnd());
    }
    if (view.getSnapTo() != 0) {
      event.setStart(((int)(event.getUnsnappedStart() / view.getSnapTo())) * view.getSnapTo());
      event.setEnd(((int)(event.getUnsnappedEnd() / view.getSnapTo())) * view.getSnapTo());
    } else {
      event.setStart(event.getUnsnappedStart());
      event.setEnd(event.getUnsnappedEnd());
    }
    
    // Store the coordinates where it was picked up.
    double time = view.getTimeAt(physicalX);
    double note = view.getNoteAt(physicalY);
    previousTime_ = time;
    previousNote_ = note;
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
   * Called on finger down.
   */
  private void onTouchDown(ScoreView view, int physicalX, int physicalY) {
    double time = view.getTimeAt(physicalX);
    double note = view.getNoteAt(physicalY);

    // When we're done editing this item, this tool stays selected.
    nextTool_ = this;

    // See if there's an event being touched.
    Event.Builder event = view.getEventAt(physicalX, physicalY);
    if (event != null) {
      if (!event.getSelected()) {
        // De-select everything.
        for (int j = 0; j < view.getScore().getEventCount(); ++j) {
          view.getScore().getEventBuilder(j).setSelected(false);
        }
        // Select just the one that was pressed.
        event.setSelected(true);
      }

      // Compute the handle size.
      int physicalHandleSize = view.getNoteY(0) - view.getNoteY(1);
      double handleWidth = view.getTimeAt(physicalHandleSize) - view.getTimeAt(0);
      boolean largeEnough = event.getEnd() - event.getStart() > handleWidth * 2;

      // See if any handle was touched.
      if (largeEnough && time <= event.getStart() + handleWidth) {
        mode_ = RESIZING_LEFT;
      } else if (largeEnough && time >= event.getEnd() - handleWidth) {
        mode_ = RESIZING_RIGHT;
      } else {
        mode_ = MOVING;
      }
    } else {
      // De-select everything.
      for (int j = 0; j < view.getScore().getEventCount(); ++j) {
        view.getScore().getEventBuilder(j).setSelected(false);
      }

      selection_.left = physicalX;
      selection_.right = physicalX;
      selection_.top = physicalY;
      selection_.bottom = physicalY;

      mode_ = SELECTING;
    }
    view.invalidate();
    previousTime_ = time;
    previousNote_ = note;
  }

  /**
   * Called on finger movements.
   */
  private void onTouchMove(ScoreView view, int physicalX, int physicalY) {
    double time = view.getTimeAt(physicalX);
    double note = view.getNoteAt(physicalY);
    double deltaTime = time - previousTime_;
    double deltaNote = note - previousNote_;
    switch (mode_) {
      case RESIZING_RIGHT: {
        for (int i = 0; i < view.getScore().getEventCount(); ++i) {
          Event.Builder event = view.getScore().getEventBuilder(i);
          if (!event.getSelected()) {
            continue;
          }

          if (!event.hasUnsnappedEnd()) {
            event.setUnsnappedEnd(event.getEnd());
          }
          event.setUnsnappedEnd(event.getUnsnappedEnd() + deltaTime);
          if (view.getSnapTo() != 0) {
            event.setEnd(((int)(event.getUnsnappedEnd() / view.getSnapTo())) * view.getSnapTo());
          } else {
            event.setEnd(event.getUnsnappedEnd());
          }

          if (event.getEnd() <= event.getStart()) {
            event.setEnd(event.getStart() + 1/32.0f);
          }
        }
        break;
      }
      case RESIZING_LEFT: {
        for (int i = 0; i < view.getScore().getEventCount(); ++i) {
          Event.Builder event = view.getScore().getEventBuilder(i);
          if (!event.getSelected()) {
            continue;
          }

          if (!event.hasUnsnappedStart()) {
            event.setUnsnappedStart(event.getStart());
          }
          event.setUnsnappedStart(event.getUnsnappedStart() + deltaTime);
          if (view.getSnapTo() != 0) {
            event.setStart(((int)(event.getUnsnappedStart() / view.getSnapTo())) * view.getSnapTo());
          } else {
            event.setStart(event.getUnsnappedStart());
          }

          if (event.getStart() >= event.getEnd()) {
            event.setEnd(event.getEnd() - 1/32.0f);
          }
        }
        break;
      }
      case MOVING: {
        for (int i = 0; i < view.getScore().getEventCount(); ++i) {
          Event.Builder event = view.getScore().getEventBuilder(i);
          if (!event.getSelected()) {
            continue;
          }

          if (!event.hasUnsnappedStart()) {
            event.setUnsnappedStart(event.getStart());
          }
          event.setUnsnappedStart(event.getUnsnappedStart() + deltaTime);
          if (view.getSnapTo() != 0) {
            event.setStart(((int)(event.getUnsnappedStart() / view.getSnapTo())) *
                           view.getSnapTo());
          } else {
            event.setStart(event.getUnsnappedStart());
          }

          if (!event.hasUnsnappedEnd()) {
            event.setUnsnappedEnd(event.getEnd());
          }
          event.setUnsnappedEnd(event.getUnsnappedEnd() + deltaTime);
          if (view.getSnapTo() != 0) {
            event.setEnd(((int)(event.getUnsnappedEnd() / view.getSnapTo())) * view.getSnapTo());
          } else {
            event.setEnd(event.getUnsnappedEnd());
          }

          if (event.getEnd() <= event.getStart()) {
            event.setEnd(event.getStart() + 1/32.0f);
          }

          if (!event.hasUnsnappedKey()) {
            event.setUnsnappedKey(event.getKey());
          }
          event.setUnsnappedKey(event.getUnsnappedKey() + deltaNote);
          event.setKey((int)event.getUnsnappedKey());

          trashVisible_ = true;
          trashRect_.top = view.getDrawingRect().top;
          trashRect_.bottom = view.getDrawingRect().top +
              Math.max(200, trashIcon_.getIntrinsicHeight());
          trashRect_.left = view.getDrawingRect().right -
              Math.max(200, trashIcon_.getIntrinsicWidth());
          trashRect_.right = view.getDrawingRect().right;
          trashIcon_.setBounds(trashRect_);
        }
        break;
      }
      case SELECTING: {
        // Update the selection rectangle.
        selection_.right = physicalX;
        selection_.bottom = physicalY;

        // Update event selections.
        for (int i = 0; i < view.getScore().getEventCount(); ++i) {
          Event.Builder event = view.getScore().getEventBuilder(i);
          if (selection_.intersects(view.getTimeX(event.getStart()),
                                    view.getNoteY(event.getKey() + 1),
                                    view.getTimeX(event.getEnd()),
                                    view.getNoteY(event.getKey()))) {
            view.getScore().getEventBuilder(i).setSelected(true);
          } else {
            view.getScore().getEventBuilder(i).setSelected(false);
          }
        }

        break;
      }
    }
    view.invalidate();
    previousTime_ = time;
    previousNote_ = note;
  }

  /**
   * Called on finger up.
   */
  private void onTouchUp(ScoreView view, int physicalX, int physicalY) {
    trashVisible_ = false;

    if (trashRect_.contains(physicalX, physicalY)) {
      for (int i = view.getScore().getEventCount() - 1; i >= 0; --i) {
        if (view.getScore().getEventBuilder(i).getSelected()) {
          view.getScore().removeEvent(i);
        }
      }
    }

    // Make all snapping permanent.
    for (int i = 0; i < view.getScore().getEventCount(); ++i) {
      Event.Builder event = view.getScore().getEventBuilder(i);
      event.clearUnsnappedStart();
      event.clearUnsnappedEnd();
      event.clearUnsnappedKey();
    }

    mode_ = NONE;
    view.setTool(nextTool_);

    view.invalidate();
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
      pointerId_ = event.getPointerId(0);
      onTouchDown(view, (int)event.getX(), (int)event.getY());
      return true;
    } else if (actionCode == MotionEvent.ACTION_POINTER_DOWN) {
      return false;
    } else if (actionCode == MotionEvent.ACTION_MOVE) {
      // Find the current positions of the fingers.
      for (int pointerIndex = 0; pointerIndex < event.getPointerCount(); ++pointerIndex) {
        int pointerId = event.getPointerId(pointerIndex);
        if (pointerId >= 0 && pointerId == pointerId_) {
          int physicalX = (int)event.getX(pointerIndex);
          int physicalY = (int)event.getY(pointerIndex);
          onTouchMove(view, physicalX, physicalY);
          return true;
        }
      }
      return false;
    } else if (actionCode == MotionEvent.ACTION_UP) {
      onTouchUp(view, (int)event.getX(), (int)event.getY());
      return true;
    } else if (actionCode == MotionEvent.ACTION_POINTER_UP) {
      return false;
    } else {
      return false;
    }
  }

  /**
   * Called after each event is drawn, to give this tool a chance to draw over it.
   * See ScoreView.onDraw() for more information on how ScoreView is drawn.
   * @param event - The event that was drawn.
   * @param canvas - The canvas the key is drawn into.
   * @param rect - The area of the key on the canvas.
   */
  @Override
  public void afterDrawEvent(Event event,
                             Canvas canvas,
                             Rect rect) {
    if (rect.right >= rect.left + rect.height() * 2) {
      // Draw left arrow.
      float margin = 1.0f;
      paint_.setStrokeWidth(1.0f);
      paint_.setStyle(Paint.Style.STROKE);
      if (event.hasKeyEvent()) {
        paint_.setColor(Color.BLACK);
      } else {
        paint_.setColor(Color.WHITE);
      }
      canvas.drawRect(rect.left, rect.top,
                      rect.left + rect.height(), rect.top + rect.height(),
                      paint_);
      paint_.setStyle(Paint.Style.FILL);
      path_.reset();
      path_.moveTo(rect.left + rect.height() - margin, rect.top + margin);
      path_.lineTo(rect.left + rect.height() - margin, rect.bottom - margin);
      path_.lineTo(rect.left + margin, (rect.top + rect.bottom) / 2.0f);
      path_.close();
      canvas.drawPath(path_, paint_);

      // Draw right arrow.
      paint_.setStyle(Paint.Style.STROKE);
      if (event.hasKeyEvent()) {
        paint_.setColor(Color.BLACK);
      } else {
        paint_.setColor(Color.WHITE);
      }
      canvas.drawRect(rect.right - rect.height(), rect.top, rect.right, rect.bottom, paint_);
      paint_.setStyle(Paint.Style.FILL);
      path_.reset();
      path_.moveTo(rect.right - (rect.height() - (margin + 1)), rect.top + margin);
      path_.lineTo(rect.right - (rect.height() - (margin + 1)), rect.bottom - margin);
      path_.lineTo(rect.right - margin, (rect.top + rect.bottom) / 2.0f);
      path_.close();
      canvas.drawPath(path_, paint_);
    }
  }

  /**
   * Called after the entire score is drawn, to give this tool a chance to draw over it.
   * Draws the selection box, and possibly the trash icon.
   * See ScoreView.onDraw() for more information on how ScoreView is drawn.
   * @param view - The ScoreView being drawn.
   * @param canvas - The canvas the key is drawn into.
   * @param rect - The area of the key on the canvas.
   */
  @Override
  public void afterDrawScore(ScoreView view, Canvas canvas, Rect rect) {
    if (mode_ == SELECTING) {
      paint_.setStyle(Paint.Style.FILL);
      paint_.setColor(Color.CYAN);
      paint_.setAlpha(127);
      canvas.drawRect(selection_, paint_);
      paint_.setAlpha(255);
    }

    // Draw the trash can.
    if (trashVisible_) {
      trashIcon_.draw(canvas);
    }
  }

  // The id of the finger doing the editing.
  private int pointerId_;

  // The most recent previous position of the finger.
  private double previousTime_;
  private double previousNote_;

  // A tool to select the next time the user finishes editing an event.
  // Can be "this", but not null.
  private ScoreViewTool nextTool_;

  // While the user is drawing a selection rectangle, this is it, in screen (physical) coordinates.
  private Rect selection_;

  // Some objects used in drawing.  They are owned here so that they don't have to be reallocated
  // and garbage collected for every pass of drawing.
  protected Paint paint_;
  protected Path path_;
  private Drawable icon_;

  // The mode of the tool, depending mostly on where the user's finger was when they first touched.
  private int mode_;
  private static final int NONE = 0;
  private static final int RESIZING_LEFT = 1;
  private static final int RESIZING_RIGHT = 2;
  private static final int MOVING = 3;
  private static final int SELECTING = 4;

  // Members for controlling how the trash can icon gets drawn while moving events.
  private boolean trashVisible_;
  private Rect trashRect_;  // in screen (physical) coordinates.
  private Drawable trashIcon_;

  @SuppressWarnings("unused")
  private Logger logger_;
}
