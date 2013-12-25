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
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader.TileMode;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.levien.synthesizer.R;
import com.levien.synthesizer.core.model.SynthesizerInput;
import com.levien.synthesizer.core.model.composite.MultiChannelSynthesizer;
import com.levien.synthesizer.core.model.composite.Presets.Setting;

/**
 * KnobView is a widget for setting a real value by turning a virtual "knob".
 */
public class KnobView extends View {
  /** Basic constructor for an Android widget. */
  public KnobView(Context context, AttributeSet attrs) {
    super(context, attrs);

    // Load the xml attributes.
    TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.KnobView);
    knobValue_ = a.getFloat(R.styleable.KnobView_value, 0.5f);
    min_ = a.getFloat(R.styleable.KnobView_min, 0.0f);
    max_ = a.getFloat(R.styleable.KnobView_max, 1.0f);
    a.recycle();

    // Set up the drawing structures.
    knobPaint_ = new Paint();
    knobPaint_.setAntiAlias(true);
    knobPaint_.setColor(Color.WHITE);
    float density = getResources().getDisplayMetrics().density;
    knobPaint_.setStrokeWidth(2.0f * density);
    rect_ = new Rect();
    rectF_ = new RectF();
    textRect_ = new Rect();

    // The listener has to be set later.
    listener_ = null;

    int padding = (int)(3.0 * density + 0.5);
    setPadding(padding, padding, padding, padding);
  }

  /**
   * Touch event handler.
   */
  @Override
  public boolean onTouchEvent(MotionEvent event) {
    int action = event.getAction();
    switch (action) {
      case MotionEvent.ACTION_DOWN: {
        // Just record the current finger position.
        getDrawingRect(rect_);
        previousX_ = event.getX() - rect_.centerX();
        previousY_ = event.getY() - rect_.centerY();
        currentTouchAngle_ = knobValue_ * 2 * Math.PI * 0.8 + (Math.PI / 5.0);
        diffAngle_ = 0;
        break;
      }

      case MotionEvent.ACTION_MOVE: {
        getDrawingRect(rect_);

        // Compare the previous angle of the finger position (relative to the center of the control)
        // to the new angle, and update the value accordingly.
        currentTouchAngle_ = knobValue_ * 2 * Math.PI * 0.8 + (Math.PI / 5.0);
        currentX_ = event.getX() - rect_.centerX();
        currentY_ = event.getY() - rect_.centerY();
        diffAngle_ = Math.atan2(currentY_, currentX_) - Math.atan2(previousY_, previousX_);
        if (diffAngle_ > Math.PI) {
          diffAngle_ -= Math.PI * 2;
        } else if (diffAngle_ < -Math.PI) {
          diffAngle_ += Math.PI * 2;
        }
        currentTouchAngle_ += diffAngle_;
        knobValue_ = currentTouchAngle_ / (2.0 * Math.PI);
        if (knobValue_ < 0.1) knobValue_ = 0.1;
        if (knobValue_ > 0.9) knobValue_ = 0.9;
        knobValue_ -= 0.1;
        knobValue_ /= 0.8;
        previousX_ = currentX_;
        previousY_ = currentY_;

        // Notify listener and redraw.
        if (listener_ != null) {
          listener_.onKnobChanged(getValue());
        }
        invalidate();

        break;
      }

      case MotionEvent.ACTION_UP: {
        break;
      }
    }
    return true;
  }

  /**
   * Sets the listener to receive events when the knob's value changes.
   */
  public void setKnobListener(KnobListener listener) {
    listener_ = listener;
  }

  /**
   * Sets the value for the knob when it is turned all the way counter-clockwise.
   */
  public void setMin(double min) {
    min_ = min;
    invalidate();
  }

  /**
   * Sets the value for the knob when it is turned all the way clockwise.
   */
  public void setMax(double max) {
    max_ = max;
    invalidate();
  }

  /**
   * Sets the current value of the knob. Note that this call does not
   * invoke the listener. The assumption is that any caller will also
   * update the client.
   */
  public void setValue(double value) {
    if (value < min_) {
      knobValue_ = 0.0;
    } else if (value > max_) {
      knobValue_ = 1.0;
    } else {
      knobValue_ = (value - min_) / (max_ - min_);
    }
    invalidate();
  }

  /**
   * Returns the current value of the knob.
   */
  public double getValue() {
    return min_ + (knobValue_ * (max_ - min_));
  }

  /**
   * Drawing handler.
   */
  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);

    getDrawingRect(rect_);
    rectF_.set(rect_);
    // Make it square.
    if (rectF_.height() > rectF_.width()) {
      float center = rectF_.centerY();
      rectF_.top = center - rectF_.width() / 2;
      rectF_.bottom = center + rectF_.width() / 2;
    } else {
      float center = rectF_.centerX();
      rectF_.left = center - rectF_.height() / 2;
      rectF_.right = center + rectF_.height() / 2;
    }

    // Draw outer white glow.
    int[] radialGradientColors = {Color.WHITE,  Color.WHITE, 0x00000000};
    float[] radialGradientPositions = {0.0f, 0.87f, 1.0f};
    radialGradient_ = new RadialGradient(rect_.exactCenterX(),
                                         rect_.exactCenterY(),
                                         Math.min(rect_.width(), rect_.height()) / 2,
                                         radialGradientColors,
                                         radialGradientPositions,
                                         TileMode.CLAMP);

    knobPaint_.setShader(radialGradient_);
    canvas.drawCircle(rect_.exactCenterX(),
                      rect_.exactCenterY(),
                      Math.min(rect_.width(), rect_.height()) / 2,
                      knobPaint_);

    // Draw outer gauge.
    final int fullDark = Color.BLACK;
    final int guageStartColor = 0xff202050;
    final int guageEndColor = 0xff4040A0;

    final int adjustedStartColor = Color.argb(
        0xFF,
        (int)(0.1875 * Color.red(guageStartColor) + (1.0 - 0.1875) * Color.red(guageEndColor)),
        (int)(0.1875 * Color.green(guageStartColor) + (1.0 - 0.1875) * Color.green(guageEndColor)),
        (int)(0.1875 * Color.blue(guageStartColor) + (1.0 - 0.1875) * Color.blue(guageEndColor)));

    int[] sweepGradientColors = {
        adjustedStartColor,
        guageEndColor,
        fullDark,
        fullDark,
        guageStartColor,
        adjustedStartColor};
    float[] sweepGradientPositions = { 0.0f, 0.16f, 0.16f, 0.35f, 0.35f, 1.0f };
    sweepGradient_ = new SweepGradient(rect_.exactCenterX(),
                                       rect_.exactCenterY(),
                                       sweepGradientColors,
                                       sweepGradientPositions);
    knobPaint_.setShader(sweepGradient_);
    canvas.drawCircle(rect_.exactCenterX(),
                      rect_.exactCenterY(),
                      Math.min(rect_.width(), rect_.height()) / 2.4f,
                      knobPaint_);

    // Draw inner gauge.
    knobPaint_.setShader(null);
    knobPaint_.setStyle(Style.FILL);
    knobPaint_.setColor(Color.BLACK);
    canvas.drawCircle(rect_.exactCenterX(),
                      rect_.exactCenterY(),
                      Math.min(rect_.width(), rect_.height()) / 4,
                      knobPaint_);

    // Draw inner white glow.
    int[] innerRadialGradientColors = { 0x00000000,  0x00000000, Color.WHITE };
    float[] innerRadialGradientPositions = { 0.0f, 0.6f, 1.0f };
    innerRadialGradient_ = new RadialGradient(rect_.exactCenterX(),
                                              rect_.exactCenterY(),
                                              Math.min(rect_.width(), rect_.height()) / 4f,
                                              innerRadialGradientColors,
                                              innerRadialGradientPositions,
                                              TileMode.CLAMP);
    knobPaint_.setShader(innerRadialGradient_);
    canvas.drawCircle(rect_.exactCenterX(),
                      rect_.exactCenterY(),
                      Math.min(rect_.width(),rect_.height()) / 4,
                      knobPaint_);

    // Draw indicator.
    knobPaint_.setShader(null);
    knobPaint_.setColor(Color.WHITE);
    knobPaint_.setStyle(Style.STROKE);
    final float arcWidth = 15.0f;
    canvas.drawArc(rectF_,
                   (float)(knobValue_ * 360 * 0.8 + 90 - arcWidth / 2 + 36),
                   (float)arcWidth,
                   false,
                   knobPaint_);

    // Draw text.
    String knobValueString = String.format("%.2f", getValue());
    Typeface typeface = Typeface.create(knobPaint_.getTypeface(), Typeface.BOLD);
    knobPaint_.setTypeface(typeface);
    knobPaint_.setTextAlign(Align.CENTER);
    knobPaint_.setTextSize(rectF_.width() / 8);
    knobPaint_.setSubpixelText(true);
    knobPaint_.setStyle(Style.FILL);
    knobPaint_.getTextBounds(knobValueString, 0, knobValueString.length(), textRect_);
    canvas.drawText(knobValueString,
                    rect_.centerX(),
                    rect_.centerY() + textRect_.height() / 2,
                    knobPaint_);
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
   * Connects knob to a SynthesizerInput.
   * @input - The synthesizer input to connect to.
   */
  public void bindTo(final SynthesizerInput input) {
    setValue(input.getSynthesizerInputValue());
    setKnobListener(new KnobListener() {
      public void onKnobChanged(double newValue) {
        input.setValue(newValue);
      }
    });
  }

  /**
   * Connects knob to a SynthesizerInput.
   * @synth - The synthesizer to connect to.
   * @path - The setting to connect to.
   * @return - True on success, false on failure.
   */
  public boolean bindTo(final MultiChannelSynthesizer synth, int channel, Setting setting) {
    SynthesizerInput input = synth.getChannel(channel).getSynthesizerInput(setting);
    if (input != null) {
      bindTo(input);
      return true;
    } else {
      Log.e(getClass().getName(), "Unable to bind to setting " + setting.name() + ".");
      return false;
    }
  }

  // Knob's current value, ranges from 0 - 1.0.
  private double knobValue_;
  private double min_;
  private double max_;

  // Structures used in drawing that we don't want to reallocate every time we draw.
  private Paint knobPaint_;
  private Rect rect_;
  private Rect textRect_;
  private RectF rectF_;
  private SweepGradient sweepGradient_;
  private RadialGradient radialGradient_;
  private RadialGradient innerRadialGradient_;

  // Position of the finger relative to the knob.
  private double previousX_ = 0;
  private double previousY_ = 0;
  private double currentX_ = 0;
  private double currentY_ = 0;
  private double currentTouchAngle_ = 0;
  private double diffAngle_ = 0;

  // Object listening for events when the knob's value changes.
  private KnobListener listener_;
}
