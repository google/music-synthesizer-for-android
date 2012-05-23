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

package com.google.synthesizer.android.widgets.piano;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.google.synthesizer.R;
import com.google.synthesizer.core.midi.MidiListener;
import com.google.synthesizer.core.model.composite.MultiChannelSynthesizer;
import com.google.synthesizer.core.music.Note;

/**
 * PianoView is a UI widget that simulates a music keyboard.
 */
public class PianoView extends View {
  /**
   * Basic android widget constructor.
   */
  public PianoView(Context context, AttributeSet attrs) {
    super(context, attrs);

    // Get the xml attributes for this instance.
    TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PianoView);
    octaves_ = a.getInteger(R.styleable.PianoView_octaves, 1);
    firstOctave_ = a.getInteger(R.styleable.PianoView_first_octave, 4);

    // Set up basic drawing structs, just so we don't have to allocate this later when we draw.
    drawingRect_ = new Rect();

    // Generate the set of keys.  There are 12 music keys per octave, plus the octave change button
    // on either end.
    keys_ = new PianoKey[12 * octaves_ + 2];
    int key = 0;

    // Create the white keys.
    for (int octave = 0; octave < octaves_; ++octave) {
      for (int note = 0; note < 7; ++note) {
        keys_[key++] = new WhitePianoKey(this, octave, note);
      }
    }

    // Create the black keys.
    for (int octave = 0; octave < octaves_; ++octave) {
      for (int note = 0; note < 7; ++note) {
        if (BlackPianoKey.isValid(note)) {
          keys_[key++] = new BlackPianoKey(this, octave, note);
        }
      }
    }

    // Create the octave changing keys.
    keys_[key++] = new OctavePianoKey(this, -1);
    keys_[key++] = new OctavePianoKey(this, 1);

    // The listener will have to be set later.
    pianoViewListener_ = null;
  }

  /**
   * Returns the absolute octave of the left-most key.
   */
  public int getFirstOctave() {
    return firstOctave_;
  }

  /**
   * Returns the number of octaves covered by all of the keys.
   */
  public int getOctaves() {
    return octaves_;
  }

  /**
   * Shifts the octave of all of the keys.
   * @param delta - The number (and direction) of octaves to shift by.
   */
  public void changeOctave(int delta) {
    firstOctave_ += delta;
  }

  /**
   * Sets the listener that will receive events from this widget.
   */
  public void setPianoViewListener(PianoViewListener pianoViewListener) {
    pianoViewListener_ = pianoViewListener;
  }

  /**
   * Signals the listener that a new note was pressed.
   * @param logFrequency - the log frequency of the new note.
   * @param retriggerIfOn - true if this is a new touch, rather than just moving.
   */
  private void notifyNoteDown(double logFrequency, int finger, boolean retriggerIfOn,
          float pressure) {
    if (pianoViewListener_ != null) {
      pianoViewListener_.noteDown(logFrequency, finger, retriggerIfOn, pressure);
    }
  }

  /**
   * Signals the listener that a note was released.
   */
  private void notifyNoteUp(int finger) {
    if (pianoViewListener_ != null) {
      pianoViewListener_.noteUp(finger);
    }
  }

  /**
   * Draws the widget.
   */
  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    getDrawingRect(drawingRect_);
    for (int i = 0; i < keys_.length; ++i) {
      keys_[i].layout(drawingRect_, octaves_);
    }
    for (int i = 0; i < keys_.length; ++i) {
      keys_[i].draw(canvas);
    }
  }

  /**
   * Called to handle touch down events.
   * Returns true iff we need to redraw.
   */
  protected boolean onTouchDown(int finger, int x, int y, float pressure) {
    // Look through keys from top to bottom, and set the first one found as down, the rest as up.
    PianoKey keyDown = null;
    boolean redraw = false;
    for (int i = keys_.length - 1; i >= 0; --i) {
      if (keyDown != null) {
        // If we already found a key that's being touched, then none of the rest can be.
        redraw |= keys_[i].onTouchUp(finger);
      } else if (keys_[i].contains(x, y)) {
        // This key is being touched.
        redraw |= keys_[i].onTouchDown(finger);
        keyDown = keys_[i];
      } else {
        // This key is not being touched.
        redraw |= keys_[i].onTouchUp(finger);
      }
    }
    if (keyDown instanceof NotePianoKey) {
      notifyNoteDown(((NotePianoKey)keyDown).getLogFrequency(), finger, true, pressure);
    }
    return redraw;
  }

  /**
   * Called to handle touch move events.
   */
  protected boolean onTouchMove(int finger, int x, int y, float pressure) {
    // Look through keys from top to bottom, and set the first one found as moved, the rest as up.
    PianoKey keyDown = null;
    boolean redraw = false;
    boolean wasPressed = false;
    for (int i = keys_.length - 1; i >= 0; --i) {
      if (keyDown != null) {
        // If we already found a key that's being touched, then none of the rest can be.
        redraw |= keys_[i].onTouchUp(finger);
      } else if (keys_[i].contains(x, y)) {
        // This key is being pressed.
        wasPressed = keys_[i].isPressed();
        redraw |= keys_[i].onTouchMoved(finger);
        keyDown = keys_[i];
      } else {
        // This key is not being pressed.
        redraw |= keys_[i].onTouchUp(finger);
      }
    }
    if (keyDown instanceof NotePianoKey) {
      if (!usePressure_) {
        pressure = 0.5f;
      }
      if (!wasPressed) {
        notifyNoteDown(((NotePianoKey)keyDown).getLogFrequency(), finger, false, pressure);
      }
    } else {
      notifyNoteUp(finger);
    }
    return redraw;
  }

  /**
   * Called to handle touch up events.
   */
  protected boolean onTouchUp(int finger) {
    // Set all keys as up.
    boolean redraw = false;
    for (int i = 0; i < keys_.length; ++i) {
      redraw |= keys_[i].onTouchUp(finger);
    }
    notifyNoteUp(finger);
    return redraw;
  }
  

  /**
   * Handler for all touch events.
   */
  @Override
  public boolean onTouchEvent(MotionEvent event) {
    int action = event.getAction();
    int actionCode = action & MotionEvent.ACTION_MASK;
    boolean redraw = false;
    if (actionCode == MotionEvent.ACTION_DOWN) {
      int pointerId = event.getPointerId(0);
      if (pointerId < FINGERS) {
        int x = (int)event.getX();
        int y = (int)event.getY();
        float pressure = event.getPressure();
        redraw |= onTouchDown(pointerId, x, y, pressure);
      }
    } else if (actionCode == MotionEvent.ACTION_POINTER_DOWN) {
      int pointerIndex = (action & MotionEvent.ACTION_POINTER_INDEX_MASK)
          >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
      int pointerId = event.getPointerId(pointerIndex);
      if (pointerId < FINGERS && pointerId >= 0) {
        int x = (int)event.getX(pointerIndex);
        int y = (int)event.getY(pointerIndex);
        float pressure = event.getPressure(pointerIndex);
        redraw |= onTouchDown(pointerId, x, y, pressure);
      }
    } else if (actionCode == MotionEvent.ACTION_MOVE) {
      for (int pointerIndex = 0; pointerIndex < event.getPointerCount(); ++pointerIndex) {
        int pointerId = event.getPointerId(pointerIndex);
        if (pointerId >= FINGERS) {
          continue;
        }
        if (pointerIndex >= 0) {
          int x = (int)event.getX(pointerIndex);
          int y = (int)event.getY(pointerIndex);
          float pressure = event.getPressure(pointerIndex);
          redraw |= onTouchMove(pointerId, x, y, pressure);
        }
      }
    } else if (actionCode == MotionEvent.ACTION_UP) {
      int pointerId = event.getPointerId(0);
      if (pointerId < FINGERS) {
        redraw |= onTouchUp(pointerId);
      }
      // Clean up any other pointers that have disappeared.
      for (pointerId = 0; pointerId < FINGERS; ++pointerId) {
        boolean found = false;
        for (int pointerIndex = 0; pointerIndex < event.getPointerCount(); ++pointerIndex) {
          if (pointerId == event.getPointerId(pointerIndex)) {
            found = true;
            break;
          }
        }
        if (!found) {
          redraw |= onTouchUp(pointerId);
        }
      }
    } else if (actionCode == MotionEvent.ACTION_POINTER_UP) {
      int pointerIndex = (action & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
      int pointerId = event.getPointerId(pointerIndex);
      if (pointerId < FINGERS) {
        redraw |= onTouchUp(pointerId);
      }
      // Clean up any other pointers that have disappeared. Note: this is probably not necessary.
      for (pointerId = 0; pointerId < FINGERS; ++pointerId) {
        boolean found = false;
        for (pointerIndex = 0; pointerIndex < event.getPointerCount(); ++pointerIndex) {
          if (pointerId == event.getPointerId(pointerIndex)) {
            found = true;
            break;
          }
        }
        if (!found) {
          redraw |= onTouchUp(pointerId);
        }
      }
    } else {
      return super.onTouchEvent(event);
    }
    if (redraw) {
      invalidate();
    }
    return true;    
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

  /**
   * Connects the PianoView to a Synthesizer.
   * @synth - The synthesizer to connect to.
   * @channel - Which of the synthesizer's channels to bind to.
   */
  public void bindTo(final MultiChannelSynthesizer synth, final int channel) {
    this.setPianoViewListener(new PianoViewListener() {
      public void noteDown(double logFrequency, int finger, boolean retriggerIfOn,
              float pressure) {
        synth.getChannel(channel).setPitch(logFrequency, finger);
        synth.getChannel(channel).turnOn(retriggerIfOn, finger);
      }
      public void noteUp(int finger) {
        synth.getChannel(channel).turnOff(finger);
      }
    });
  }

  /**
   * Connects the PianoView to an MidiListener.
   */
  public void bindTo(final MidiListener midiListener) {
    this.setPianoViewListener(new PianoViewListener() {
      {
        fingerMap_ = new HashMap<Integer, Integer>();
      }
      public void noteDown(double logFrequency, int finger, boolean retriggerIfOn,
              float pressure) {
        noteUp(finger);
        int midiNote = Note.getKeyforLog12TET(logFrequency);
        fingerMap_.put(finger, midiNote);
        int midiPressure = Math.max(1, Math.min(127, (int)(127 * pressure)));
        midiListener.onNoteOn(0, midiNote, midiPressure);
      }
      public void noteUp(int finger) {
        if (fingerMap_.containsKey(finger)) {
          int midiNote = fingerMap_.get(finger);
          fingerMap_.remove(finger);
          midiListener.onNoteOff(0, midiNote, 64);
        }
      }
      private Map<Integer, Integer> fingerMap_;
    });
  }

  // The most recent screen rect that this keyboard was drawn into.
  //
  // This is basically a stack variable for onDraw.  It's a member variable only so that we can
  // avoid reallocating them every time the keyboard is redrawn.
  private Rect drawingRect_;

  // The set of keys on the keyboard.
  private PianoKey[] keys_;

  // The current octave the keyboard is on.
  private int firstOctave_;

  // The total number of octaves the keyboard displays at any one time.
  private final int octaves_;

  // The listener to receive key events.
  private PianoViewListener pianoViewListener_;

  // The number of simultaneous fingers supported by this control.
  protected static final int FINGERS = 5;
  
  // Whether to use pressure (doesn't work well on all hardware)
  private boolean usePressure_ = true;
}
