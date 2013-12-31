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
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class ScrollStripView extends View {
  public ScrollStripView(Context context, AttributeSet attrs) {
    super(context, attrs);
    drawingRect_ = new Rect();
    paint_ = new Paint();
    paint_.setAntiAlias(true);
    paint_.setStyle(Paint.Style.FILL);
    paint_.setColor(Color.GRAY);
    touchDown_ = new boolean[2];
    touchx_ = new float[2];
    zoom_ = -1f;  // placeholder value to be replaced in onLayout
  }

  public void bindKeyboard(KeyboardView kv) {
    keyboardView_ = kv;
  }

  @Override
  public void onLayout(boolean changed, int left, int top, int right, int bottom) {
    if (zoom_ < 0) {
      float density = getResources().getDisplayMetrics().density;
      int width = right - left;
      zoom_ = 3000f * density / width;
      offset_ = -width * 0.5f * (zoom_ - 1);
      if (keyboardView_ != null) {
        keyboardView_.setScrollZoom(offset_, zoom_);
      }
    }
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    float density = getResources().getDisplayMetrics().density;
    getDrawingRect(drawingRect_);
    float y = drawingRect_.centerY();
    float spacing = 10f * density * zoom_;
    int min = -(int)(offset_ / spacing);
    int max = -(int)((offset_ - drawingRect_.width()) / spacing) + 1;
    for (int i = min; i <= max; i++) {
      float x = offset_ + i * spacing;
      canvas.drawCircle(x, y, 3.0f * density, paint_);
    }
  }

  private boolean onTouchDown(int id, float x) {
    touchx_[id] = x;
    if (!touchDown_[0] && !touchDown_[1]) {
      deltaAtTouch_ = offset_ - x;
    } else {
      deltaAtTouch_ = offset_ - touchx_[0];
      zoomAtTouch_ = zoom_;
      scaleAtTouch_ = zoom_ / (touchx_[1] - touchx_[0]);
    }
    touchDown_[id] = true;
    return false;
  }

  private boolean onTouchUp(int id) {
    touchDown_[id] = false;
    if (touchDown_[1 - id]) {
      // Going from 2-finger (zoom) back to 1-finger (scroll)
      deltaAtTouch_ = offset_ - touchx_[1 - id];
    }
    return false;
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    int actionCode = event.getActionMasked();
    boolean redraw = false;
    if (actionCode == MotionEvent.ACTION_DOWN) {
      int pointerId = event.getPointerId(0);
      if (pointerId < 2 && pointerId >= 0) {
        redraw = onTouchDown(pointerId, event.getX());
      }
    } else if (actionCode == MotionEvent.ACTION_POINTER_DOWN) {
      int pointerIndex = event.getActionIndex();
      int pointerId = event.getPointerId(pointerIndex);
      if (pointerId < 2 && pointerId >= 0) {
        redraw = onTouchDown(pointerId, event.getX(pointerIndex));
      }
    } else if (actionCode == MotionEvent.ACTION_UP) {
      int pointerId = event.getPointerId(0);
      if (pointerId < 2 && pointerId >= 0) {
        onTouchUp(pointerId);
      }
    } else if (actionCode == MotionEvent.ACTION_POINTER_UP) {
      int pointerIndex = event.getActionIndex();
      int pointerId = event.getPointerId(pointerIndex);
      if (pointerId < 2 && pointerId >= 0) {
        onTouchUp(pointerId);
      }
    } else if (actionCode == MotionEvent.ACTION_MOVE) {
      for (int pointerIndex = 0; pointerIndex < event.getPointerCount(); pointerIndex++) {
        int pointerId = event.getPointerId(pointerIndex);
        if (pointerId < 2 && pointerId >= 0) {
          touchx_[pointerId] = event.getX(pointerIndex);
        }
      }
      if (touchDown_[0] && !touchDown_[1]) {
        offset_ = touchx_[0] + deltaAtTouch_;
        redraw = true;
      } else if (!touchDown_[0] && touchDown_[1]) {
        offset_ = touchx_[1] + deltaAtTouch_;
        redraw = true;
      } else if (touchDown_[0] && touchDown_[1]) {
        zoom_ = scaleAtTouch_ * (touchx_[1] - touchx_[0]);
        // TODO: min and max zoom values maybe should depend on screen size
        // Also, zoom range should be on the larger side for 3 level layout
        zoom_ = Math.max(1.0f, zoom_);
        zoom_ = Math.min(5.0f, zoom_);
        offset_ = touchx_[0] + zoom_ / zoomAtTouch_ * deltaAtTouch_;
        redraw = true;
      }
      offset_ = Math.max(-keyboardView_.getMaxScroll(), offset_);
      offset_ = Math.min(0, offset_);
    }
    if (redraw) {
      if (keyboardView_ != null) {
        keyboardView_.setScrollZoom(offset_, zoom_);
      }
      invalidate();
    }
    return true;
  }

  Rect drawingRect_;
  Paint paint_;
  float offset_;
  float zoom_;

  // touch state
  boolean[] touchDown_;
  float[] touchx_;
  float deltaAtTouch_;
  float zoomAtTouch_;
  float scaleAtTouch_;

  KeyboardView keyboardView_;
}
