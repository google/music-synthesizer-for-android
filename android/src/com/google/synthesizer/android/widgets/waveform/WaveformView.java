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

package com.google.synthesizer.android.widgets.waveform;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import com.google.synthesizer.core.model.WaveformInput;
import com.google.synthesizer.core.model.composite.MultiChannelSynthesizer;
import com.google.synthesizer.core.model.composite.Presets.Setting;

/**
 * WaveformView is a control for selecting from among available waveforms.
 * It's designed to occupy the same space as a KnobView.
 */
public class WaveformView extends View {
  /** Basic constructor for an Android widget. */
  public WaveformView(Context context, AttributeSet attrs) {
    super(context, attrs);

    waveform_ = WaveformInput.SINE;

    // Set up the drawing structures.
    paint_ = new Paint();
    path_ = new Path();
    rect_ = new Rect();

    // The listener has to be set later.
    listener_ = null;

    setPadding(3, 3, 3, 3);
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
        double y = (event.getY() - rect_.top) / rect_.height();
        if (x < 0.5) {
          if (y < 0.34) {
            setWaveform(WaveformInput.SINE);
          } else if (y < 0.67) {
            setWaveform(WaveformInput.TRIANGLE);
          } else {
            setWaveform(WaveformInput.SQUARE);
          }
        } else {
          if (y < 0.34) {
            setWaveform(WaveformInput.SAWTOOTH);
          } else if (y < 0.67) {
            setWaveform(WaveformInput.NOISE);
          }
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
   * Sets the listener to receive events when the value changes.
   */
  public void setWaveformListener(WaveformListener listener) {
    listener_ = listener;
  }

  /**
   * Sets the current value of the knob.
   */
  public void setWaveform(String waveform) {
    waveform_ = waveform;
    if (listener_ != null) {
      listener_.onWaveformChanged(waveform);
    }
    invalidate();
  }

  /**
   * Returns the current value of the knob.
   */
  public String getWaveform() {
    return waveform_;
  }

  /**
   * Draws a button for selecting a sine waveform.
   */
  protected void drawSine(Canvas canvas,
                          float x, float y,
                          float width, float height,
                          float margin,
                          float lineWidth) {
    int steps = 12;

    // Sine wave.
    path_.reset();
    path_.moveTo(x, y + (height / 2));
    for (int i = 0; i < steps + 1; i++) {
      float x1 = x + (i / (float)steps) * width;
      float y1 = y + -1 * (height/2) * (float)Math.sin(2.0f/steps * Math.PI * i) + height/2;
      path_.lineTo(x1, y1);
    }
    paint_.setColor(Color.WHITE);
    if (waveform_.equals(WaveformInput.SINE)) {
      paint_.setStyle(Paint.Style.FILL);
      canvas.drawRect(x - margin / 2,
                      y - margin / 2,
                      x + width + margin / 2,
                      y + height + margin / 2,
                      paint_);
      paint_.setColor(Color.BLACK);
    }
    paint_.setStyle(Paint.Style.STROKE);
    paint_.setStrokeWidth(lineWidth);
    paint_.setStrokeJoin(Paint.Join.ROUND);
    canvas.drawPath(path_, paint_);
  }

  /**
   * Draws a button for selecting a triangle waveform.
   */
  protected void drawTriangle(Canvas canvas,
                              float x, float y,
                              float width, float height,
                              float margin,
                              float lineWidth) {
    // Triangle Wave.
    path_.reset();
    path_.moveTo(x, y + (height / 2));
    path_.lineTo(x + width / 4, y);
    path_.lineTo(x + width * (3.0f / 4.0f), y + height);
    path_.lineTo(x + width, y + (height / 2));
    paint_.setColor(Color.WHITE);
    if (waveform_.equals(WaveformInput.TRIANGLE)) {
      paint_.setStyle(Paint.Style.FILL);
      canvas.drawRect(x - margin / 2,
                      y - margin / 2,
                      x + width + margin / 2,
                      y + height + margin / 2,
                      paint_);
      paint_.setColor(Color.BLACK);
    }
    paint_.setStyle(Paint.Style.STROKE);
    paint_.setStrokeWidth(lineWidth);
    paint_.setStrokeJoin(Paint.Join.ROUND);
    canvas.drawPath(path_, paint_);
  }

  /**
   * Draws a button for selecting a square waveform.
   */
  protected void drawSquare(Canvas canvas,
                            float x, float y,
                            float width, float height,
                            float margin,
                            float lineWidth) {
    // Square Wave.
    path_.reset();
    path_.moveTo(x, y + height);
    path_.lineTo(x + width / 4, y + height);
    path_.lineTo(x + width / 4, y);
    path_.lineTo(x + width * (3.0f / 4.0f), y);
    path_.lineTo(x + width * (3.0f / 4.0f), y + height);
    path_.lineTo(x + width, y + height);
    paint_.setColor(Color.WHITE);
    if (waveform_.equals(WaveformInput.SQUARE)) {
      paint_.setStyle(Paint.Style.FILL);
      canvas.drawRect(x - margin / 2,
                      y - margin / 2,
                      x + width + margin / 2,
                      y + height + margin / 2,
                      paint_);
      paint_.setColor(Color.BLACK);
    }
    paint_.setStyle(Paint.Style.STROKE);
    paint_.setStrokeWidth(lineWidth);
    paint_.setStrokeJoin(Paint.Join.ROUND);
    canvas.drawPath(path_, paint_);
  }

  /**
   * Draws a button for selecting a sawtooth waveform.
   */
  protected void drawSawtooth(Canvas canvas,
                              float x, float y,
                              float width, float height,
                              float margin,
                              float lineWidth) {
    // Sawtooth Wave.
    path_.reset();
    path_.moveTo(x, y + height);
    path_.lineTo(x, y);
    path_.lineTo(x + width / 2, y + height);
    path_.lineTo(x + width / 2, y);
    path_.lineTo(x + width, y + height);
    paint_.setColor(Color.WHITE);
    if (waveform_.equals(WaveformInput.SAWTOOTH)) {
      paint_.setStyle(Paint.Style.FILL);
      canvas.drawRect(x - margin / 2,
                      y - margin / 2,
                      x + width + margin / 2,
                      y + height + margin / 2,
                      paint_);
      paint_.setColor(Color.BLACK);
    }
    paint_.setStyle(Paint.Style.STROKE);
    paint_.setStrokeWidth(lineWidth);
    paint_.setStrokeJoin(Paint.Join.ROUND);
    canvas.drawPath(path_, paint_);
  }

  /**
   * Draws a button for selecting a noise waveform.
   */
  protected void drawNoise(Canvas canvas,
                           float x, float y,
                           float width, float height,
                           float margin,
                           float lineWidth) {
    // Noise.
    path_.reset();
    path_.moveTo(x, y + height * 0.5f);
    path_.lineTo(x + 0.125f * width, y + height * 0.4f);
    path_.lineTo(x + 0.25f * width, y + height * 1.0f);
    path_.lineTo(x + 0.375f * width, y + height * 0.3f);
    path_.lineTo(x + 0.5f * width, y + height * 0.7f);
    path_.lineTo(x + 0.625f * width, y + height * 0.0f);
    path_.lineTo(x + 0.75f * width, y + height * 0.8f);
    path_.lineTo(x + 0.875f * width, y + height * 0.2f);
    path_.lineTo(x + 1.0f * width, y + height * 0.5f);
    paint_.setColor(Color.WHITE);
    if (waveform_.equals(WaveformInput.NOISE)) {
      paint_.setStyle(Paint.Style.FILL);
      canvas.drawRect(x - margin / 2,
                      y - margin / 2,
                      x + width + margin / 2,
                      y + height + margin / 2,
                      paint_);
      paint_.setColor(Color.BLACK);
    }
    paint_.setStyle(Paint.Style.STROKE);
    paint_.setStrokeWidth(lineWidth);
    paint_.setStrokeJoin(Paint.Join.ROUND);
    canvas.drawPath(path_, paint_);
  }

  /**
   * Draws a button for selecting a Karplus-Strong waveform.
   */
  protected void drawOther(Canvas canvas,
                           float x, float y,
                           float width, float height,
                           float margin,
                           float lineWidth) {
    int steps = 12;

    paint_.setColor(Color.WHITE);
    if (!waveform_.equals(WaveformInput.SINE) &&
        !waveform_.equals(WaveformInput.TRIANGLE) &&
        !waveform_.equals(WaveformInput.SAWTOOTH) &&
        !waveform_.equals(WaveformInput.SQUARE) &&
        !waveform_.equals(WaveformInput.NOISE)) {
      paint_.setStyle(Paint.Style.FILL);
      canvas.drawRect(x - margin / 2,
                      y - margin / 2,
                      x + width + margin / 2,
                      y + height + margin / 2,
                      paint_);
      paint_.setColor(Color.BLACK);
    }
    paint_.setStyle(Paint.Style.STROKE);
    paint_.setStrokeWidth(lineWidth);
    paint_.setStrokeJoin(Paint.Join.ROUND);

    path_.reset();
    path_.moveTo(x, y + (height / 2));
    for (int i = 0; i < steps + 1; i++) {
      float x1 = x + (i / (float)steps) * width;
      float y1 = y + -1 * (height/2) * (float)Math.sin(2.0f/steps * Math.PI * i) + height/2;
      path_.lineTo(x1, y1);
    }
    canvas.drawPath(path_, paint_);

    path_.reset();
    path_.moveTo(x, y + (height / 2));
    for (int i = 0; i < steps + 1; i++) {
      float x1 = x + (i / (float)steps) * width;
      float y1 = y + -0.6f * (height/2) * (float)Math.sin(2.0f/steps * Math.PI * (steps-i)) + height/2;
      path_.lineTo(x1, y1);
    }
    canvas.drawPath(path_, paint_);
  }
    
  /**
   * Drawing handler.
   */
  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);

    getDrawingRect(rect_);
    rect_.set(rect_);
    // Make it square.
    if (rect_.height() > rect_.width()) {
      int center = rect_.centerY();
      rect_.top = center - rect_.width() / 2;
      rect_.bottom = center + rect_.width() / 2;
    } else {
      int center = rect_.centerX();
      rect_.left = center - rect_.height() / 2;
      rect_.right = center + rect_.height() / 2;
    }

    paint_.setColor(Color.BLACK);
    paint_.setStyle(Paint.Style.FILL);
    canvas.drawRect(rect_, paint_);

    // Draw waveforms.
    float lineWidth = 5.0f;
    float margin = 15.0f;
    float waveWidth = (rect_.width() - 3.0f * margin) / 2.0f;
    float waveHeight = (rect_.height() - 4.0f * margin) / 3.0f;

    float xOffset = margin;
    float yOffset = margin;
    drawSine(canvas, xOffset, yOffset, waveWidth, waveHeight, margin, lineWidth);
    yOffset += waveHeight;
    yOffset += margin;
    drawTriangle(canvas, xOffset, yOffset, waveWidth, waveHeight, margin, lineWidth);
    yOffset += waveHeight;
    yOffset += margin;
    drawSquare(canvas, xOffset, yOffset, waveWidth, waveHeight, margin, lineWidth);
    yOffset = margin;
    xOffset += waveWidth;
    xOffset += margin;
    drawSawtooth(canvas, xOffset, yOffset, waveWidth, waveHeight, margin, lineWidth);
    yOffset += waveHeight;
    yOffset += margin;
    drawNoise(canvas, xOffset, yOffset, waveWidth, waveHeight, margin, lineWidth);
    yOffset += waveHeight;
    yOffset += margin;
    drawOther(canvas, xOffset, yOffset, waveWidth, waveHeight, margin, lineWidth);
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

    // Specify that 100 is preferred for both dimensions.
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

  /**
   * Connects control to a WaveformInput.
   * @input - The synthesizer input to connect to.
   */
  public void bindTo(WaveformInput waveform) {
    input_ = waveform;
    setWaveform(waveform.getWaveform(waveform.getSelected()));
    setWaveformListener(new WaveformListener() {
      public void onWaveformChanged(String newValue) {
        input_.select(newValue);
      }
    });
  }

  /**
   * Connects control to a WaveformSelector module.
   * @synth - The synthesizer to connect to.
   * @setting - The setting to connect to.
   * @return - True on success, false on failure.
   */
  public boolean bindTo(final MultiChannelSynthesizer synth, int channel, Setting setting) {
    WaveformInput input = synth.getChannel(0).getWaveformInput(setting);
    if (input != null) {
      bindTo(input);
      return true;
    } else {
      Log.e(getClass().getName(), "Unable to bind to setting " + setting.name() + ".");
      return false;
    }
  }

  // Currently selected waveform.
  private String waveform_;
  protected WaveformInput input_;

  // Structures used in drawing that we don't want to reallocate every time we draw.
  protected Paint paint_;
  protected Path path_;
  protected Rect rect_;

  // Object listening for events when the knob's value changes.
  private WaveformListener listener_;
}