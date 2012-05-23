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

package com.google.synthesizer.android.widgets;

import com.google.synthesizer.R;
import com.google.synthesizer.android.widgets.piano.PianoViewListener;
import com.google.synthesizer.core.model.composite.MultiChannelSynthesizer;
import com.google.synthesizer.core.music.Note;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * ChordGridView is an alternative interface for performing that has keys for chords amongst
 * fundamentals arranged in a circle of fifths.
 */
public class ChordGridView extends View {
  /**
   * Basic android widget constructor.
   */
  public ChordGridView(Context context, AttributeSet attrs) {
    super(context, attrs);

    // Get the xml attributes for this instance.
    TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ChordGridView);
    firstOctave_ = a.getInteger(R.styleable.ChordGridView_octave, 4);

    pressedRow_ = -1;
    pressedColumn_ = -1;

    drawingRect_ = new Rect();
    path_ = new Path();
    strokePaint_ = new Paint();
    fillPaint_ = new Paint();
    strokePaint_.setStyle(Style.STROKE);
    fillPaint_.setStyle(Style.FILL);
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
  private void notifyNoteDown(double logFrequency, int finger, boolean retriggerIfOn) {
    if (pianoViewListener_ != null) {
      pianoViewListener_.noteDown(logFrequency, finger, retriggerIfOn, 1.0f);
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
   * Handler for all touch events.
   */
  @Override
  public boolean onTouchEvent(MotionEvent event) {
    int action = event.getAction();
    int actionCode = action & MotionEvent.ACTION_MASK;
    boolean redraw = false;
    if (actionCode == MotionEvent.ACTION_DOWN) {
      int tileWidth = drawingRect_.width() / COLUMNS;
      int tileHeight = drawingRect_.height() / ROWS;
      pressedColumn_ = (int)(event.getX() - drawingRect_.left) / tileWidth;
      pressedRow_ = (int)(event.getY() - drawingRect_.top) / tileHeight;      
      redraw = true;

      getTileInfo(pressedRow_, pressedColumn_);
      notifyNoteDown(Note.computeLog12TET(tileNote1_, tileOctave1_), 0, true);
      notifyNoteDown(Note.computeLog12TET(tileNote2_, tileOctave2_), 1, true);
      notifyNoteDown(Note.computeLog12TET(tileNote3_, tileOctave3_), 2, true);
    } else if (actionCode == MotionEvent.ACTION_MOVE) {
      int tileWidth = drawingRect_.width() / COLUMNS;
      int tileHeight = drawingRect_.height() / ROWS;
      int newPressedColumn_ = (int)(event.getX() - drawingRect_.left) / tileWidth;
      int newPressedRow_ = (int)(event.getY() - drawingRect_.top) / tileHeight;      
      if (pressedColumn_ != newPressedColumn_ || pressedRow_ != newPressedRow_) {
        pressedColumn_ = newPressedColumn_;
        pressedRow_ = newPressedRow_;
        redraw = true;

        getTileInfo(pressedRow_, pressedColumn_);
        notifyNoteDown(Note.computeLog12TET(tileNote1_, tileOctave1_), 0, false);
        notifyNoteDown(Note.computeLog12TET(tileNote2_, tileOctave2_), 1, false);
        notifyNoteDown(Note.computeLog12TET(tileNote3_, tileOctave3_), 2, false);
      }
    } else if (actionCode == MotionEvent.ACTION_UP) {
      pressedColumn_ = -1;
      pressedRow_ = -1;

      notifyNoteUp(0);
      notifyNoteUp(1);
      notifyNoteUp(2);
      redraw = true;
    } else {
      return super.onTouchEvent(event);
    }
    if (redraw) {
      invalidate();
    }
    return true;    
  }

  /**
   * Draws the widget.
   */
  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    getDrawingRect(drawingRect_);
    int tileWidth = drawingRect_.width() / COLUMNS;
    int tileHeight = drawingRect_.height() / ROWS;
    for (int row = 0; row < ROWS; ++row) {
      for (int column = 0; column < COLUMNS; ++column) {
        getTileInfo(row, column);
        int foreground;
        int background;
        if (Note.isNatural(tileNote1_)) {
          if (row == pressedRow_ && column == pressedColumn_) {
            foreground = Color.GREEN;
          } else {
            foreground = Color.WHITE;
          }
          background = Color.BLACK;
        } else {
          foreground = Color.BLACK;
          if (row == pressedRow_ && column == pressedColumn_) {
            background = Color.GREEN;
          } else {
            background = Color.WHITE;
          }
        }
        int textColor = background;
        fillPaint_.setColor(foreground);
        strokePaint_.setColor(background);
        canvas.drawRect(tileWidth * column,
                        tileHeight * row,
                        tileWidth * (column + 1),
                        tileHeight * (row + 1),
                        fillPaint_);
        canvas.drawRect(tileWidth * column + 1,
                        tileHeight * row + 1,
                        tileWidth * (column + 1) - 2,
                        tileHeight * (row + 1) - 2,
                        strokePaint_);
        if (tileIsMajor_) {
          textColor = foreground;
          strokePaint_.setColor(foreground);
          fillPaint_.setColor(background);
          path_.reset();
          path_.moveTo(tileWidth * column + 15, tileHeight * (row + 1) - 15);
          path_.lineTo(tileWidth * (column + 1) - 15, tileHeight * (row + 1) - 15);
          path_.lineTo(tileWidth * column + tileWidth / 2, tileHeight * row + 15);
          path_.close();
          canvas.drawPath(path_, fillPaint_);
        }
        if (tileIsMinor_) {
          textColor = foreground;
          strokePaint_.setColor(foreground);
          fillPaint_.setColor(background);
          path_.reset();
          path_.moveTo(tileWidth * column + 15, tileHeight * row + tileHeight / 2);
          path_.lineTo(tileWidth * (column + 1) - 15, tileHeight * (row + 1) - 15);
          path_.lineTo(tileWidth * (column + 1) - 15, tileHeight * row + 15);
          path_.close();
          canvas.drawPath(path_, fillPaint_);
        }
        if (tileNote1_ == Note.C && !tileIsMinor_ && !tileIsMajor_) {
          fillPaint_.setColor(background);
          canvas.drawCircle(tileWidth * column + tileWidth / 2,
                            tileHeight * row + tileHeight / 2,
                            10,
                            fillPaint_);
        }
        strokePaint_.setColor(textColor);
        canvas.drawText(Note.getName(tileNote1_),
                        tileWidth * column + tileWidth / 2,
                        tileHeight * row + tileHeight / 2,
                        strokePaint_);
      }
    } 
  }

  /**
   * Populates the tile* fields for the tile at the given row and column.
   */
  private void getTileInfo(int row, int column) {
    int startIndex = 5 + firstOctave_ * 36;
    int absoluteIndex = startIndex + row + column * 14;
    if (absoluteIndex % 3 == 0) {
      // Fundamental key.
      tileOctave1_ = absoluteIndex / 36;
      tileNote1_ = (absoluteIndex % 36) / 3;
      tileOctave2_ = tileOctave1_;
      tileNote2_ = tileNote1_;
      tileOctave3_ = tileOctave1_;
      tileNote3_ = tileNote1_;
      tileIsMajor_ = false;
      tileIsMinor_ = false;
    } else if (absoluteIndex % 3 == 1) {
      // Minor chord key.
      tileOctave3_ = (int)((absoluteIndex - 1) / 36) + 1;
      tileNote3_ = ((absoluteIndex - 1) % 36) / 3;
      if (tileNote3_ >= 4) {
        tileOctave2_ = tileOctave3_;
        tileNote2_ = tileNote3_ - 4;
      } else {
        tileOctave2_ = tileOctave3_ - 1;
        tileNote2_ = tileNote3_ + 8;
      }
      if (tileNote3_ >= 7) {
        tileOctave1_ = tileOctave3_;
        tileNote1_ = tileNote3_ - 7;
      } else {
        tileOctave1_ = tileOctave3_ - 1;
        tileNote1_ = tileNote3_ + 5;
      }
      tileIsMajor_ = false;
      tileIsMinor_ = true;
    } else if (absoluteIndex % 3 == 2) {
      // Major chord key.
      tileOctave1_ = (int)((absoluteIndex + 1) / 36);
      tileNote1_ = ((absoluteIndex + 1) % 36) / 3;
      if (tileNote1_ < 8) {
        tileOctave2_ = tileOctave1_;
        tileNote2_ = tileNote1_ + 4;
      } else {
        tileOctave2_ = tileOctave1_ + 1;
        tileNote2_ = tileNote1_ - 8;
      }
      if (tileNote1_ >= 7) {
        tileOctave3_ = tileOctave1_;
        tileNote3_ = tileNote1_ + 7;
      } else {
        tileOctave3_ = tileOctave1_ + 1;
        tileNote3_ = tileNote1_ - 5;
      }
      tileIsMajor_ = true;
      tileIsMinor_ = false;
    }
  }

  /**
   * Connects the ChordGridView to a Synthesizer.
   * @synth - The synthesizer to connect to.
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
   * Populated by getTileInfo, these fields will contain the info for a particular key.
   */
  private int tileOctave1_;
  private int tileOctave2_;
  private int tileOctave3_;
  private int tileNote1_;
  private int tileNote2_;
  private int tileNote3_;
  private boolean tileIsMajor_;
  private boolean tileIsMinor_;

  // The coordinates of the key currently being pressed.
  private int pressedColumn_;
  private int pressedRow_;

  // The current octave the keyboard is on.
  private int firstOctave_;

  // The listener to receive key events.
  private PianoViewListener pianoViewListener_;

  // These are basically stack variables for onDraw.  They're member variables only so that we can
  // avoid reallocating them every time the keyboard is redrawn.
  //
  // The most recent screen rect that this keyboard was drawn into.
  private Rect drawingRect_;
  private Path path_;
  private Paint strokePaint_;
  private Paint fillPaint_;

  private static final int ROWS = 8;
  private static final int COLUMNS = 5;
}
