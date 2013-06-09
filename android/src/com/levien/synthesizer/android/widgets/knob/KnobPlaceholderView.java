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

package com.levien.synthesizer.android.widgets.knob;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

/**
 * KnobPlaceholderView is an empty widget that behaves the same way as a KnobView when sizing.
 * It's useful for layouts that vary how many knobs they have, but want them sized consistently.
 */
public class KnobPlaceholderView extends View {
  public KnobPlaceholderView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }
  
  /**
   * Controls how the knob is sized;  it is square, and prefers to be 100x100 pixels.
   */
  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int widthMode = MeasureSpec.getMode(widthMeasureSpec);
    int widthSize = MeasureSpec.getSize(widthMeasureSpec);
    int heightMode = MeasureSpec.getMode(heightMeasureSpec);
    int heightSize = MeasureSpec.getSize(heightMeasureSpec);

    // Prefer 100 for both dimensions.
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
        width = 100;
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
        height = 100;
        break;
    }

    // Make it square.
    if (width > height && widthMode != MeasureSpec.EXACTLY) {
      width = height;
    }
    if (height > width && heightMode != MeasureSpec.EXACTLY) {
      height = width;
    }
    
    setMeasuredDimension(width, height);
  }
}
