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
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.levien.synthesizer.core.midi.MidiListener;

public class KeyboardView extends View {
  public KeyboardView(Context context, AttributeSet attrs) {
    super(context, attrs);
    nKeys_ = 96;
    firstKey_ = 12;
    noteStatus_ = new byte[128];
    noteForFinger_ = new int[FINGERS];
    for (int i = 0; i < FINGERS; i++) {
      noteForFinger_[i] = -1;
    }
    drawingRect_ = new Rect();
    paint_ = new Paint();
    paint_.setAntiAlias(true);
    float density = getResources().getDisplayMetrics().density;
    textSize_ = 32.0f * density;
    paint_.setTextSize(textSize_);
    paint_.setTextAlign(Paint.Align.CENTER);
    strokeWidth_ = 1.0f * density;
    paint_.setStrokeWidth(strokeWidth_);
    offset_ = 0.0f;
    zoom_ = 1.0f;
    setKeyboardSpec(KeyboardSpec.make2Row());
    velSens_ = 0.5f;
    velAvg_ = 64;
  }

  public void setKeyboardSpec(KeyboardSpec keyboardSpec) {
    keyboardSpec_ = keyboardSpec;
    keyboardScale_ = zoom_ / keyboardSpec_.repeatWidth * keyboardSpec_.keys.length / nKeys_;
    invalidate();
  }

  public void setMidiListener(MidiListener listener) {
    midiListener_ = listener;
  }

  public void onNote(int note, int velocity) {
    if (note >= 0 && note < 128) {
      noteStatus_[note] = (byte)velocity;
      invalidate();  // could do smarter invalidation, whatev
    }
  }

  public void setVelocitySensitivity(float velSens, float velAvg) {
    velSens_ = velSens;
    velAvg_ = velAvg;
  }

  public void setScrollZoom(float offset, float zoom) {
    offset_ = offset;
    zoom_ = zoom;
    keyboardScale_ = zoom_ / keyboardSpec_.repeatWidth * keyboardSpec_.keys.length / nKeys_;
    invalidate();
  }

  public float getMaxScroll() {
    float width = drawingRect_.width();
    return (zoom_ - 1) * width;
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    getDrawingRect(drawingRect_);
    float xscale = (drawingRect_.width() - strokeWidth_) * keyboardScale_;
    float yscale = (drawingRect_.height() - strokeWidth_)  / keyboardSpec_.height;
    float x0 = drawingRect_.left + strokeWidth_ * 0.5f + offset_;
    float y0 = drawingRect_.top + strokeWidth_ * 0.5f;
    for (int i = 0; i < nKeys_; i++) {
      KeySpec ks = keyboardSpec_.keys[i % keyboardSpec_.keys.length];
      float x = x0 + ((i / keyboardSpec_.keys.length) * keyboardSpec_.repeatWidth +
              ks.rect.left) * xscale;
      float y = y0 + ks.rect.top * yscale;
      float width = ks.rect.width() * xscale;
      float height = ks.rect.height() * yscale;
      int note = i + firstKey_;
      int vel = noteStatus_[note];
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
        canvas.drawRect(x, y, x + width, y + height, paint_);
      } else {
        paint_.setColor(Color.WHITE);
      }

      // Draw note label
      if (note % 12 == 0) {
        float xs = x + width/2;
        float ys = y + 0.5f * height + 0.35f * textSize_;
        paint_.setStyle(Style.FILL);
        if (note % 12 == 0) {
          paint_.setTypeface(Typeface.DEFAULT_BOLD);
        } else {
          paint_.setTypeface(Typeface.DEFAULT);
        }
        canvas.drawText(noteString(note), xs, ys, paint_);
      }
    }
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    int actionCode = event.getActionMasked();
    boolean redraw = false;
    switch (actionCode) {
      case MotionEvent.ACTION_DOWN:
      case MotionEvent.ACTION_POINTER_DOWN:
        int index = actionCode == MotionEvent.ACTION_POINTER_DOWN ? event.getActionIndex() : 0;
        int pointerId = event.getPointerId(index);
        if (pointerId < FINGERS && pointerId >= 0) {
          float x = event.getX(index);
          float y = event.getY(index);
          float pressure = event.getPressure(index);
          redraw |= onTouchDown(pointerId, x, y, pressure);
        }
        break;
      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_POINTER_UP:
        index = actionCode == MotionEvent.ACTION_POINTER_UP ? event.getActionIndex() : 0;
        pointerId = event.getPointerId(index);
        if (pointerId < FINGERS && pointerId >= 0) {
          float x = event.getX(index);
          float y = event.getY(index);
          float pressure = event.getPressure(index);
          redraw |= onTouchUp(pointerId, x, y, pressure);
        }
        break;
      case MotionEvent.ACTION_MOVE:
        for (index = 0; index < event.getPointerCount(); index++) {
          pointerId = event.getPointerId(index);
          if (pointerId < FINGERS) {
            float x = event.getX(index);
            float y = event.getY(index);
            float pressure = event.getPressure(index);
            redraw |= onTouchMove(pointerId, x, y, pressure);
          }
        }
        break;
    }
    if (redraw) {
      invalidate();
    }
    return true;
  }

  private int hitTest(float x, float y) {
    /* convert x and y to KeyboardSpec space */
    float xscale = (drawingRect_.width() - strokeWidth_) * keyboardScale_;
    float yscale = (drawingRect_.height() - strokeWidth_)  / keyboardSpec_.height;
    float xk = (x - 0.5f * strokeWidth_ - offset_) / xscale;
    float yk = (y - 0.5f * strokeWidth_) / yscale;
    for (int i = 0; i < nKeys_; i++) {
      KeySpec ks = keyboardSpec_.keys[i % keyboardSpec_.keys.length];
      float kx0 = ks.rect.left + (i / keyboardSpec_.keys.length) * keyboardSpec_.repeatWidth;
      if (xk >= kx0 && xk < kx0 + ks.rect.width() &&
              yk >= ks.rect.top && yk < ks.rect.bottom) {
        return i + firstKey_;
      }
    }
    return -1;
  }

  private int computeVelocity(float pressure) {
    int velocity = (int) (velSens_ * (pressure - 0.5f) * 127.0f + velAvg_ + 0.5f);
    if (velocity < 1) {
      velocity = 1;
    } else if (velocity > 127) {
      velocity = 127;
    }
    return velocity;
  }

  private boolean onTouchDown(int id, float x, float y, float pressure) {
    int note = hitTest(x, y);
    if (note >= 0 && noteStatus_[note] == 0) {
      int velocity = computeVelocity(pressure);
      noteForFinger_[id] = note;
      noteStatus_[note] = (byte)velocity;
      if (midiListener_ != null) {
        midiListener_.onNoteOn(0, note, velocity);
      }
      return true;
    }
    return false;
  }

  private boolean onTouchUp(int id, float x, float y, float pressure) {
    int note = noteForFinger_[id];
    if (note >= 0) {
      int velocity = noteStatus_[note];
      if (midiListener_ != null) {
        midiListener_.onNoteOff(0, note, velocity);
      }
      noteForFinger_[id] = -1;
      noteStatus_[note] = 0;
      return true;
    }
    return false;
  }

  private boolean onTouchMove(int id, float x, float y, float pressure) {
    int oldNote = noteForFinger_[id];
    int newNote = hitTest(x, y);
    if (newNote != -1 && newNote != oldNote && noteStatus_[newNote] == 0) {
      // keep consistent velocity; new is likely to be too high
      if (oldNote >= 0) {
        int velocity = noteStatus_[oldNote];
        if (midiListener_ != null) {
          midiListener_.onNoteOff(0, oldNote, velocity);
          midiListener_.onNoteOn(0, newNote, velocity);
        }
        noteForFinger_[id] = newNote;
        noteStatus_[oldNote] = 0;
        noteStatus_[newNote] = (byte)velocity;
      } else {
        // moving onto active note from dead zone
        int velocity = 64;
        if (midiListener_ != null) {
          midiListener_.onNoteOn(0, newNote, velocity);
        }
        noteForFinger_[id] = newNote;
        noteStatus_[newNote] = (byte)velocity;
      }
      return true;
    }
  return false;
  }

  private static String noteString(int note) {
    int octave = note / 12 - 1;
    return NOTE_NAMES[note % 12] + Integer.toString(octave);
  }

  private float velSens_;
  private float velAvg_;

  private MidiListener midiListener_;

  private Rect drawingRect_;
  private Paint paint_;
  private float strokeWidth_;
  private float textSize_;
  private float keyboardScale_;

  private float offset_;
  private float zoom_;

  private KeyboardSpec keyboardSpec_;
  private int nKeys_;
  private int firstKey_;
  private byte[] noteStatus_;
  private static final int FINGERS = 10;
  private int[] noteForFinger_;
  static final String[] NOTE_NAMES = new String[] {
          "C", "C\u266f", "D", "D\u266f", "E", "F", "F\u266f", "G", "G\u266f", "A", "A\u266f", "B"
        };
}
