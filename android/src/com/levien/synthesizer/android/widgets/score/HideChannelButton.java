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

/**
 * A button that toggles between showing the events for all of the synthesizer channels or showing
 * just the currently selected channel.
 */
public class HideChannelButton extends ScoreViewTool {
  /**
   * Creates a new HideChannelButton, using the given context for loading resources.
   */
  HideChannelButton(Context context) {
    logger_ = Logger.getLogger(getClass().getName());

    visibleIcon_ = context.getResources().getDrawable(R.drawable.open_eye);
    hiddenIcon_ = context.getResources().getDrawable(R.drawable.closed_eye);
    paint_ = new Paint();
  }

  /**
   * Called when this tool is selected.
   * Changes the channel visibility and then reselects the previous tool.
   * @param view - The ScoreView that this toolbar is for.
   * @param previousTool - The tool that was selected when this one was chosen.
   */
  @Override
  public void onSelect(ScoreView view, ScoreViewTool previousTool) {
    view.setOtherChannelsVisible(!view.getOtherChannelsVisible());
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
    if (score.getOtherChannelsVisible()) {
      visibleIcon_.setBounds(rect);
      visibleIcon_.draw(canvas);
    } else {
      hiddenIcon_.setBounds(rect);
      hiddenIcon_.draw(canvas);
    }
  }

  // Some objects used in drawing.  They are owned here so that they don't have to be reallocated
  // and garbage collected for every pass of drawing.
  private Paint paint_;
  private Drawable visibleIcon_;
  private Drawable hiddenIcon_;

  @SuppressWarnings("unused")
  private Logger logger_;
}
