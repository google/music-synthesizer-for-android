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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * A toolbar that goes in a ScoreView and lets the user choose which tool to use on it.
 */
public class ScoreViewToolbar extends View implements ScoreViewListener {
  /** Basic constructor for an Android widget. */
  public ScoreViewToolbar(Context context, AttributeSet attrs) {
    super(context, attrs);

    paint_ = new Paint();
    rect_ = new Rect();
    textRect_ = new Rect();

    // Just hard-code the list of tools.
    tool_ = new ScoreViewTool[13];
    tool_[0] = new PlayButton(this, context);
    tool_[1] = new ViewportTool(context);
    tool_[2] = new EditEventTool(context);
    tool_[3] = new NewEventTool(context, (EditEventTool)tool_[2]);
    tool_[4] = new NewLoopTool(context, (EditEventTool)tool_[2]);
    tool_[5] = new HideChannelButton(context);
    tool_[6] = new PlayTool(context);
    tool_[7] = new SelectChannelButton(context, 0);
    tool_[8] = new SelectChannelButton(context, 1);
    tool_[9] = new SelectChannelButton(context, 2);
    tool_[10] = new SelectChannelButton(context, 3);
    tool_[11] = new SelectChannelButton(context, 4);
    tool_[12] = new SnapTool(context);

    toolRect_ = new Rect[tool_.length];
    for (int i = 0; i < tool_.length; ++i) {
      toolRect_[i] = new Rect();
    }
  }

  /**
   * Sets the ScoreView that this toolbar controls.
   * @param score - the ScoreView.
   */
  public void setScoreView(ScoreView score) {
    score_ = score;
    score_.setListener(this);
    score_.setTool(tool_[1]);
    invalidate();
  }

  /**
   * Sets the current tool.
   * @param tool - the tool to use.
   */
  public void onSetTool(ScoreViewTool tool) {
    invalidate();
  }

  /**
   * Touch event handler.
   */
  @Override
  public boolean onTouchEvent(MotionEvent event) {
    int action = event.getAction();
    switch (action) {
      case MotionEvent.ACTION_DOWN: {
        getDrawingRect(rect_);
        for (int i = 0; i < tool_.length; ++i) {
          if (toolRect_[i].contains((int)event.getX(), (int)event.getY())) {
            score_.setTool(tool_[i]);
            invalidate();
            break;
          }
        }
        break;
      }
      
      case MotionEvent.ACTION_MOVE: {
        break;
      }
      
      case MotionEvent.ACTION_UP: {
        break;
      }
    }
    return true;
  }

  /**
   * Drawing handler.
   */
  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);

    getDrawingRect(rect_);
    //rect_.set(rect_);

    paint_.setColor(Color.BLACK);
    paint_.setStyle(Paint.Style.FILL);
    canvas.drawRect(rect_, paint_);

    // Draw the buttons.
    float margin = 15.0f;
    float waveHeight = (rect_.height() - 2.0f * margin);
    float waveWidth = waveHeight;

    float xOffset = margin;
    float yOffset = margin;
    for (int i = 0; i < tool_.length; ++i) {
      paint_.setTextSize(24.0f);
      paint_.setColor(Color.WHITE);
      if (i == 1) {
        xOffset += 2 * margin;
        paint_.getTextBounds("Tools ", 0, 6, textRect_);
        canvas.drawText("Tools ", xOffset, yOffset + (waveHeight + textRect_.height()) / 2, paint_);
        xOffset += textRect_.width();
        xOffset += margin;
      } else if (i == 7) {
        xOffset += 2 * margin;
        paint_.getTextBounds("Instruments ", 0, 12, textRect_);
        canvas.drawText("Instruments ", xOffset, yOffset + (waveHeight + textRect_.height()) / 2, paint_);
        xOffset += textRect_.width();
        xOffset += margin;
      } else if (i == 12) {
        xOffset += 2 * margin;
        paint_.getTextBounds("Snap to ", 0, 8, textRect_);
        canvas.drawText("Snap to ", xOffset, yOffset + (waveHeight + textRect_.height()) / 2, paint_);
        xOffset += textRect_.width();
        xOffset += margin;
      }
      paint_.setColor(Color.BLACK);
      
      toolRect_[i].left = (int)xOffset;
      toolRect_[i].top = (int)yOffset;
      toolRect_[i].right = (int)(xOffset + waveWidth);
      toolRect_[i].bottom = (int)(yOffset + waveHeight);

      tool_[i].drawButton(canvas, score_, toolRect_[i], margin);

      xOffset += waveWidth;
      xOffset += margin;
    }
  }

  /**
   * Layout measurement for this widget.
   * This method just sets a basic minimum size and makes the width maximized otherwise.
   */
  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int widthMode = MeasureSpec.getMode(widthMeasureSpec);
    int widthSize = MeasureSpec.getSize(widthMeasureSpec);
    int heightMode = MeasureSpec.getMode(heightMeasureSpec);
    int heightSize = MeasureSpec.getSize(heightMeasureSpec);

    int width = 0;
    int height = 0;
    
    switch (widthMode) {
      case MeasureSpec.EXACTLY:
        width = widthSize;
        break;
      case MeasureSpec.AT_MOST:
        width = widthSize;
        break;
      case MeasureSpec.UNSPECIFIED:
        width = 10;
        break;
    }
    
    switch (heightMode) {
      case MeasureSpec.EXACTLY:
        height = heightSize;
        break;
      case MeasureSpec.AT_MOST:
        height = 150;
        break;
      case MeasureSpec.UNSPECIFIED:
        height = 10;
        break;
    }

    setMeasuredDimension(width, height);
  }

  // The score being edited.
  private ScoreView score_;

  // Structures used in drawing that we don't want to reallocate every time we draw.
  private Paint paint_;
  private Rect rect_;
  private Rect textRect_;

  // The set of available tools.
  private ScoreViewTool[] tool_;

  // The rect that each tool occupied the last time it was drawn.
  private Rect[] toolRect_;
}