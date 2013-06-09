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

package com.levien.synthesizer.android.widgets.waveform;

import com.levien.synthesizer.core.model.WaveformInput;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * WaveformRowView is like a WaveformView, but it arranges its buttons in a row.
 */
public class WaveformRowView extends WaveformView {
  /** Basic constructor for an Android widget. */
  public WaveformRowView(Context context, AttributeSet attrs) {
    super(context, attrs);
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
        double x = (event.getX() - rect_.left) / rect_.width();
        if (x < 1.0/6.0f) {
          setWaveform(WaveformInput.SINE);
        } else if (x < 2.0/6.0f) {
          setWaveform(WaveformInput.TRIANGLE);
        } else if (x < 3.0/6.0f) {
          setWaveform(WaveformInput.SQUARE);
        } else if (x < 4.0/6.0f) {
          setWaveform(WaveformInput.SAWTOOTH);
        } else if (x < 5.0/6.0f) {
          setWaveform(WaveformInput.NOISE);
        } else {
          CharSequence[] items = new CharSequence[input_.getWaveformCount()];
          for (int i = 0; i < items.length; ++i) {
            items[i] = input_.getWaveform(i);
          }
          AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
          builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
              setWaveform(input_.getWaveform(which));
            }
          });
          builder.create().show();
        }
        invalidate();
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
    rect_.set(rect_);

    paint_.setColor(Color.BLACK);
    paint_.setStyle(Paint.Style.FILL);
    canvas.drawRect(rect_, paint_);

    // Draw waveforms.
    float lineWidth = 5.0f;
    float margin = 15.0f;
    float waveWidth = (rect_.width() - 7.0f * margin) / 6.0f;
    float waveHeight = (rect_.height() - 2.0f * margin);

    float xOffset = margin;
    float yOffset = margin;
    drawSine(canvas, xOffset, yOffset, waveWidth, waveHeight, margin, lineWidth);
    xOffset += waveWidth;
    xOffset += margin;
    drawTriangle(canvas, xOffset, yOffset, waveWidth, waveHeight, margin, lineWidth);
    xOffset += waveWidth;
    xOffset += margin;
    drawSquare(canvas, xOffset, yOffset, waveWidth, waveHeight, margin, lineWidth);
    xOffset += waveWidth;
    xOffset += margin;
    drawSawtooth(canvas, xOffset, yOffset, waveWidth, waveHeight, margin, lineWidth);
    xOffset += waveWidth;
    xOffset += margin;
    drawNoise(canvas, xOffset, yOffset, waveWidth, waveHeight, margin, lineWidth);
    xOffset += waveWidth;
    xOffset += margin;
    drawOther(canvas, xOffset, yOffset, waveWidth, waveHeight, margin, lineWidth);
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
}
