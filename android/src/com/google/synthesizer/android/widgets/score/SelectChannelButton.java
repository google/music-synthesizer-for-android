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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/**
 * A tool for selecting a particular channel in a ScoreView.
 */
public class SelectChannelButton extends ScoreViewTool {
  /**
   * Creates a tool for selecting the given channel, loading resources using the given context.
   */
  SelectChannelButton(Context context, int channel) {
    logger_ = Logger.getLogger(getClass().getName());
    channel_ = channel;
    paint_ = new Paint();
  }

  /**
   * Returns the channel that this control selects.
   */
  public int getChannel() {
    return channel_;
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

    if (getChannel() == score.getCurrentChannel()) {
      paint_.setColor(score.getColorForChannel(getChannel()));
    } else {
      paint_.setColor(Color.BLACK);
    }
    paint_.setStyle(Paint.Style.FILL);
    canvas.drawRect(rect, paint_);
    Drawable icon = score.getIconForChannel(getChannel());
    icon.setBounds(rect);
    icon.draw(canvas);
  }

  /**
   * Called when this tool is selected.
   * Changes the selected channel for the view and then reselects the previously selected tool.
   * @param view - The ScoreView that this toolbar is for.
   * @param previousTool - The tool that was selected when this one was chosen.
   */
  @Override
  public void onSelect(ScoreView view, ScoreViewTool previousTool) {
    view.setCurrentChannel(channel_);
    view.setTool(previousTool);
  }

  // The channel this control selects.
  private int channel_;

  // Some objects used in drawing.  They are owned here so that they don't have to be reallocated
  // and garbage collected for every pass of drawing.
  private Paint paint_;

  @SuppressWarnings("unused")
  private Logger logger_;
}
