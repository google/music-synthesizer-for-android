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

import com.google.synthesizer.R;

import java.util.logging.Logger;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/**
 * A control for selecting the "snap to" setting of a ScoreView.
 * @see ScoreView
 */
public class SnapTool extends ScoreViewTool {
  /**
   * Creates a new SnapTool, loading resources from the given context.
   */
  SnapTool(Context context) {
    logger_ = Logger.getLogger(getClass().getName());

    customIcon_ = context.getResources().getDrawable(R.drawable.unknown_note);
    noneIcon_ = context.getResources().getDrawable(R.drawable.no_note);
    thirtySecondIcon_ = context.getResources().getDrawable(R.drawable.thirtysecond_note);
    sixteenthIcon_ = context.getResources().getDrawable(R.drawable.sixteenth_note);
    eighthIcon_ = context.getResources().getDrawable(R.drawable.eighth_note);
    quarterIcon_ = context.getResources().getDrawable(R.drawable.quarter_note);
    halfIcon_ = context.getResources().getDrawable(R.drawable.half_note);
    wholeIcon_ = context.getResources().getDrawable(R.drawable.whole_note);

    paint_ = new Paint();
  }

  /**
   * Called when this tool is selected.  Changes the "snap to" setting for the score view and then
   * reselects the previously selected tool.
   * @param view - The ScoreView that this toolbar is for.
   * @param previousTool - The tool that was selected when this one was chosen.
   */
  @Override
  public void onSelect(ScoreView view, ScoreViewTool previousTool) {
    if (view.getSnapTo() == 0.0) {
      view.setSnapTo(1.0);
    } else if (view.getSnapTo() <= 0.03125) {
      view.setSnapTo(0.0);
    } else {
      view.setSnapTo(view.getSnapTo() / 2.0);
    }
    view.setTool(previousTool);
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
    if (score.getSnapTo() == 1.0) {
      wholeIcon_.setBounds(rect);
      wholeIcon_.draw(canvas);
    } else if (score.getSnapTo() == 0.5) {
      halfIcon_.setBounds(rect);
      halfIcon_.draw(canvas);
    } else if (score.getSnapTo() == 0.25) {
      quarterIcon_.setBounds(rect);
      quarterIcon_.draw(canvas);
    } else if (score.getSnapTo() == 0.125) {
      eighthIcon_.setBounds(rect);
      eighthIcon_.draw(canvas);
    } else if (score.getSnapTo() == 0.0625) {
      sixteenthIcon_.setBounds(rect);
      sixteenthIcon_.draw(canvas);
    } else if (score.getSnapTo() == 0.03125) {
      thirtySecondIcon_.setBounds(rect);
      thirtySecondIcon_.draw(canvas);
    } else if (score.getSnapTo() == 0.0) {
      noneIcon_.setBounds(rect);
      noneIcon_.draw(canvas);
    } else {
      customIcon_.setBounds(rect);
      customIcon_.draw(canvas);
    }
  }

  // Some objects used in drawing.  They are owned here so that they don't have to be reallocated
  // and garbage collected for every pass of drawing.
  private Paint paint_;
  private Drawable customIcon_;
  private Drawable noneIcon_;
  private Drawable thirtySecondIcon_;
  private Drawable sixteenthIcon_;
  private Drawable eighthIcon_;
  private Drawable quarterIcon_;
  private Drawable halfIcon_;
  private Drawable wholeIcon_;

  @SuppressWarnings("unused")
  private Logger logger_;
}
