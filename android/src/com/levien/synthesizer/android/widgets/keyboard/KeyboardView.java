/*
 * Copyright 2013 Google Inc.
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

package com.levien.synthesizer.android.widgets.keyboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class KeyboardView extends View {
  public KeyboardView(Context context, AttributeSet attrs) {
    super(context, attrs);
    nKeys_ = 24;   // TODO: make configurable
    firstKey_ = 48;
    noteStatus_ = new byte[128];
    drawingRect_ = new Rect();
    paint_ = new Paint();
    paint_.setAntiAlias(true);
    float density = getResources().getDisplayMetrics().density;
    strokeWidth_ = 1.0f * density;
    paint_.setStrokeWidth(strokeWidth_);
  }

  public void setKeyboardSpec(KeyboardSpec keyboardSpec) {
    keyboardSpec_ = keyboardSpec;
    keyboardScale_ = 1.0f / keyboardSpec_.repeatWidth * keyboardSpec_.keys.length / nKeys_;
    invalidate();
  }

  public void onNote(int note, int velocity) {
    if (note >= 0 && note < 128) {
      noteStatus_[note] = (byte)velocity;
      invalidate();  // could do smarter invalidation, whatev
    }
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    getDrawingRect(drawingRect_);
    float xscale = (drawingRect_.width() - strokeWidth_) * keyboardScale_;
    float yscale = (drawingRect_.height() - strokeWidth_)  / keyboardSpec_.height;
    float x0 = drawingRect_.left + strokeWidth_ * 0.5f;
    float y0 = drawingRect_.top + strokeWidth_ * 0.5f;
    for (int i = 0; i < nKeys_; i++) {
      KeySpec ks = keyboardSpec_.keys[i % keyboardSpec_.keys.length];
      Log.d("synth", "ks["+i+"] = " + ks);
      float x = x0 + ((i / keyboardSpec_.keys.length) * keyboardSpec_.repeatWidth +
              ks.rect.left) * xscale;
      float y = y0 + ks.rect.top * yscale;
      float width = ks.rect.width() * xscale - strokeWidth_;
      float height = ks.rect.height() * yscale - strokeWidth_;
      int vel = noteStatus_[i + firstKey_];
      if (vel == 0) {
        paint_.setColor(ks.color);
      } else {
        // green->yellow->red gradient, dependent on velocity
        int color;
        if (vel < 64) {
          color = 0xff00ff00 + (vel << 18);
        } else {
          color = 0xffffff00 - ((vel - 64) << 10);
        }
        paint_.setColor(color);
      }
      paint_.setStyle(Style.FILL);
      canvas.drawRect(x, y, x + width, y + height, paint_);
      if (ks.color != Color.BLACK) {
        paint_.setColor(Color.BLACK);
        paint_.setStyle(Style.STROKE);
        canvas.drawRect(x, y, x + width + strokeWidth_, y + height + strokeWidth_, paint_);
      }
    }
  }

  /**
   * Layout measurement for this widget.
   * This method just sets a basic minimum size and makes the widget maximized otherwise.
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
        height = heightSize;
        break;
      case MeasureSpec.UNSPECIFIED:
        height = 10;
        break;
    }

    setMeasuredDimension(width, height);
  }

  private Rect drawingRect_;
  private Paint paint_;
  private float strokeWidth_;
  private float keyboardScale_;

  private KeyboardSpec keyboardSpec_;
  private int nKeys_;
  private int firstKey_;
  private byte[] noteStatus_;
}
