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

package com.levien.synthesizer.android.widgets.score;

import java.util.logging.Logger;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.levien.synthesizer.R;
import com.levien.synthesizer.core.midi.MidiListener;
import com.levien.synthesizer.core.music.Music.Event;
import com.levien.synthesizer.core.music.Music.Score;
import com.levien.synthesizer.core.music.Note;

/**
 * ScoreView is a UI widget that allows editing a musical score, as well as live playing.  The
 * majority of the ScoreView area shows a subsequence of the current musical score.  Along the
 * y-axis are the keys of a piano.  Time is along the x-axis.  Along the bottom, there is a toolbar,
 * which allows selecting various "tools" to use on the score.
 *
 * A score is composed of various "events", such as playing a note for a certain duration at a
 * certain time, using a certain channel.  A ScoreView lets the user create or edit these events.
 *
 * PlayTool - When selected, pressing plays the note at that x using the selected channel.
 * ViewportTool - Sets the currently visible part of the score by touching or dragging.
 * NewEventTool - Creates new events.
 * EditEventTool - Edits an existing event.
 * PlayButton - Starts the score playing audibly.
 * SelectChannelButton - Selects a particular channel (instrument) for editing or playing.
 * HideChannelButton - Toggles whether to show/hide the channels not currently being edited.
 * SnapTool - Changes the "snap to" setting for this ScoreView.
 */
public class ScoreView extends View {
  /**
   * Basic android widget constructor.
   */
  public ScoreView(Context context, AttributeSet attrs) {
    super(context, attrs);

    logger_ = Logger.getLogger(getClass().getName());

    // Set the default time to be 20 measures, and show 5 measures to start.
    minTime_ = 0.0;
    maxTime_ = 20.0;
    timeZoom_ = 0.25;
    timeOffset_ = 0.0;

    // Set the piano keys to the range of a normal piano, showing one octave to start.
    minNote_ = Note.A;
    maxNote_ = Note.A + 88.0;
    noteZoom_ = 8.0 / 88.0;
    noteOffset_ = 44.0;

    // Snap to eighth notes to start.
    snapTo_ = 1.0 / 8.0;

    // Create the score to edit.
    score_ = Score.newBuilder();

    // Setup the channels.
    currentChannel_ = 0;
    showOtherChannels_ = true;

    // Load the icon to use for each channel.
    iconForChannel_ = new Drawable[CHANNELS];
    iconForChannel_[0] = context.getResources().getDrawable(R.drawable.guitar);
    iconForChannel_[1] = context.getResources().getDrawable(R.drawable.bass);
    iconForChannel_[2] = context.getResources().getDrawable(R.drawable.voice);
    iconForChannel_[3] = context.getResources().getDrawable(R.drawable.flute);
    iconForChannel_[4] = context.getResources().getDrawable(R.drawable.drums);

    arrowsVisible_ = true;
    upSelected_ = false;
    downSelected_ = false;
    upIcon_ = context.getResources().getDrawable(R.drawable.up);
    downIcon_ = context.getResources().getDrawable(R.drawable.down);

    // Set up basic drawing structs, just so we don't have to allocate them later when we draw.
    drawingRect_ = new Rect();
    keyRect_ = new Rect();
    eventRect_ = new Rect();
    fillPaint_ = new Paint();
    strokePaint_ = new Paint();
    marginPaint_ = new Paint();

    fillPaint_.setStyle(Paint.Style.FILL);
    strokePaint_.setStyle(Paint.Style.STROKE);
    marginPaint_.setStyle(Paint.Style.FILL);
    marginPaint_.setColor(Color.GRAY);
  }

  /**
   * Returns a mutable copy of the score being edited.
   */
  public Score.Builder getScore() {
    return score_;
  }

  /**
   * Returns the currently selected channel (instrument).
   */
  public int getCurrentChannel() {
    return currentChannel_;
  }

  /**
   * Selects the given channel.
   * @param channel - The channel to select.
   */
  public void setCurrentChannel(int channel) {
    currentChannel_ = channel;
  }

  /**
   * Returns true iff the given channel is visible in the score.
   */
  public boolean isChannelVisible(int channel) {
    if (showOtherChannels_) {
      return true;
    } else {
      return channel == currentChannel_;
    }
  }

  /**
   * Returns true iff channels that aren't the current one are visible in the score.
   */
  public boolean getOtherChannelsVisible() {
    return showOtherChannels_;
  }

  /**
   * Sets whether channels that aren't the current one are visible in the score.
   */
  public void setOtherChannelsVisible(boolean visible) {
    showOtherChannels_ = visible;
  }

  /**
   * Returns the currently selected tool for this ScoreView.
   */
  public ScoreViewTool getTool() {
    return currentTool_;
  }

  /**
   * Sets a tool to be the current tool for this ScoreView.  Informs listeners of the change.
   */
  public void setTool(ScoreViewTool tool) {
    ScoreViewTool previousTool = currentTool_;
    currentTool_ = tool;
    tool.onSelect(this, previousTool);
    if (listener_ != null) {
      listener_.onSetTool(tool);
    }
    invalidate();
  }

  /**
   * Returns the "snap to" setting for this ScoreView.  @see setSnapTo().
   * @return the note that should be snapped to.  For example, if editing should snap to the nearest
   * quarter note, then returns 0.25.  For a whole note, 1.0.  For no snapping, returns 0.0.
   */
  public double getSnapTo() {
    return snapTo_;
  }

  /**
   * Sets the "snap to" setting for this ScoreView.  @see getSnapTo().
   * @param snapTo - the note that should be snapped to.  For example, if editing should snap to the
   * nearest quarter note, then 0.25.  For a whole note, 1.0.  For no snapping, 0.0.
   */
  public void setSnapTo(double snapTo) {
    snapTo_ = snapTo;
  }

  /**
   * Returns the zoom setting for this control on the x-axis.  @see setTimeZoom().
   * @return the multiplier for the x-axis on the viewport.  1.0 means the entire time of the score
   * is visible.  0.5 means only half of the score (time-wise) is visible.  2.0 means that the
   * entire score is shown, but only takes up half the screen.  Any excess space is just "margin".
   */
  public double getTimeZoom() {
    return timeZoom_;
  }

  /**
   * Sets the zoom level for this control on the x-axis.  @see getTimeZoom().
   * @param zoom - the multiplier for the x-axis on the viewport.  1.0 means the entire time of the
   * score is visible.  0.5 means only half of the score (time-wise) is visible.  2.0 means that the
   * entire score is shown, but only takes up half the screen.  Any excess space is just "margin".
   */
  public void setTimeZoom(double zoom) {
    timeZoom_ = zoom;
  }

  /**
   * Returns the zoom setting for this control on the y-axis.  @see setNoteZoom().
   * @return the multiplier for the y-axis on the viewport, which controls how many note keys are
   * visible.  1.0 means show the entire 88 keys of the piano are visible.  N/88 means exactly N
   * keys are visible.  Values larger than 1.0 means extra "margin" is shown at the top and bottom.
   */
  public double getNoteZoom() {
    return noteZoom_;
  }

  /**
   * Sets the zoom setting for this control on the y-axis.  @see getNoteZoom().
   * @param zoom - the multiplier for the y-axis on the viewport, which controls how many note keys
   * are visible.  1.0 means show the entire 88 keys of the piano are visible.  N/88 means exactly N
   * keys are visible.  Values larger than 1.0 means extra "margin" is shown at the top and bottom.
   */
  public void setNoteZoom(double zoom) {
    noteZoom_ = zoom;
  }

  /**
   * Returns the left-most time currently visible in this control.  @see setTimeOffset().
   * @return the time, in measures, from the beginning of the score to the first visible time
   * in the ScoreView.  For example, 5.25 in 4/4 time would mean one quarter note past the end of
   * the 5th measure.  Negative values mean margin is shown on the left side.
   */
  public double getTimeOffset() {
    return timeOffset_;
  }

  /**
   * Sets the left-most time currently visible in this control.  @see getTimeOffset().
   * @param offset - The time, in measures, from the beginning of the score to the first visible
   * time in the ScoreView.  For example, 5.25 in 4/4 time would mean one quarter note past the end
   * of the 5th measure.  Negative values mean margin is shown on the left side.
   */
  public void setTimeOffset(double offset) {
    timeOffset_ = offset;
  }

  /**
   * Returns the bottom-most note key currently visible in this control.  @see setNoteOffset().
   * @return the note number of the bottom key visible on the screen.  0.0 means the lowest note is
   * fully visible, with its bottom along the bottom edge of the screen.  1.0 means the lowest note
   * is not visible, but its top edge is along the bottom of the screen.  88.0 means no keys are
   * visible, but the top edge of the highest key is along the bottom of the screen.
   */
  public double getNoteOffset() {
    return noteOffset_;
  }

  /**
   * Sets the bottom-most note key currently visible in this control.  @see getNoteOffset().
   * @param offset - the note number of the bottom key visible on the screen.  0.0 means the lowest
   * note is fully visible, with its bottom along the bottom edge of the screen.  1.0 means the
   * lowest note is not visible, but its top edge is along the bottom of the screen.  88.0 means no
   * keys are visible, but the top edge of the highest key is along the bottom of the screen.
   */
  public void setNoteOffset(double offset) {
    noteOffset_ = offset;
  }

  /**
   * Returns the max time viewable or editable by this ScoreView.  @see setMaxTime().
   * @return the time, where 0.0 means no time, 1.0 means one measure, and 10 means ten measures.
   */
  public double getMaxTime() {
    return maxTime_;
  }

  /**
   * Sets the max time viewable or editable by this ScoreView.  @see getMaxTime().
   * @param max - the time, where 0.0 means no time, 1.0 means one measure, and 10 for ten measures.
   */
  public void setMaxTime(double max) {
    maxTime_ = max;
  }

  /**
   * Returns the max possible note viewable or editable by this ScoreView.  @see setMaxNote().
   * @return the note number.  For a normal piano layout, this method should always return 88.0.
   */
  public double getMaxNote() {
    return maxNote_;
  }

  /**
   * Sets the max possible note viewable or editable by this ScoreView.  @see getMaxNote().
   * @param max - the note number.  For a normal piano layout, this should always be 88.0.
   */
  public void setMaxNote(double max) {
    maxNote_ = max;
  }

  /**
   * Returns the rectangle, in screen coordinates, where this ScoreView was most recently drawn.
   * @return a reference to the Rect.
   */
  public Rect getDrawingRect() {
    return drawingRect_;
  }

  /**
   * Returns the time (logical x) that corresponds to the given pixel (physical x).
   * @param pixelX - the x in screen coordinates.
   * @return the x in logical coordinates (the time, in measures, from the score start).
   */
  public double getTimeAt(int pixelX) {
    return timeOffset_ + ((double)(pixelX - drawingRect_.left) / drawingRect_.width()) / timeZoom_;
  }

  /**
   * Returns the pixel (physical x) that corresponds to the given time (logical x).
   * @param time - the time, in measures, from the score start.
   * @return the x offset of the given time, in screen coordinates.
   */
  public int getTimeX(double time) {
    return (int)(((time - timeOffset_) * timeZoom_) * drawingRect_.width() +
                 drawingRect_.left + 0.5);
  }

  /**
   * Returns the note (logical y) that corresponds to the given pixel (physical y).
   * @param pixelY - the y in screen coordinates.
   * @return the y in logical coordinates (the note key, typically from 0 to 88.0).
   */
  public double getNoteAt(int pixelY) {
    return ((double)(drawingRect_.bottom - pixelY) / drawingRect_.height()) / noteZoom_ + noteOffset_;
  }

  /**
   * Returns the pixel (physical y) that corresponds to the given note (logical y).
   * @param note - the note key.
   * @return the y offset of the given note, in screen coordinates.
   */
  public int getNoteY(double note) {
    return (int)(drawingRect_.bottom - (note - noteOffset_) * drawingRect_.height() * noteZoom_);
  }

  /**
   * Returns the top-most event at the given coordinates.
   * @param physicalX - the x in screen coordinates.
   * @param physicalY - the y in screen coordinates.
   * @return the mutable event.
   */
  public Event.Builder getEventAt(int physicalX, int physicalY) {
    double time = getTimeAt(physicalX);
    double note = getNoteAt(physicalY);
    for (int i = score_.getEventCount() - 1; i >= 0; --i) {
      double eventStartTime = score_.getEvent(i).getStart();
      double eventEndTime = score_.getEvent(i).getEnd();
      double eventMinNote = score_.getEvent(i).getKey();
      double eventMaxNote = score_.getEvent(i).getKey() + 1;
      if (time >= eventStartTime &&
          time < eventEndTime &&
          note >= eventMinNote &&
          note < eventMaxNote) {
        return score_.getEventBuilder(i);
      }
    }
    return null;
  }

  /**
   * Sets the cursor position that shows where playback is in the score.
   * This should only be called from the Android UI thread.
   */
  public void setCursor(double cursor) {
    cursor_ = cursor;
    invalidate();
  }

  /**
   * Returns the color to use for representing the given channel in this ScoreView.
   * @param channel - the channel.
   * @return the color to use, Android style.
   */
  public int getColorForChannel(int channel) {
    switch (channel % CHANNELS) {
      case 0: return Color.rgb(0, 255, 255);
      case 1: return Color.rgb(255, 0, 255);
      case 2: return Color.rgb(255, 255, 0);
      case 3: return Color.rgb(255, 0, 0);
      case 4: return Color.rgb(0, 255, 0);
      case 5: return Color.rgb(0, 0, 255);
    }
    return Color.BLACK;
  }

  /**
   * Returns the icon to use for representing the given channel in this ScoreView.
   * @param channel - the channel.
   * @return the icon to use, as a Drawable.
   */
  public Drawable getIconForChannel(int channel) {
    return iconForChannel_[channel % iconForChannel_.length];
  }

  /**
   * Called to draw the ScoreView widget.
   * @param canvas - the canvas to draw the widget on.
   */
  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    getDrawingRect(drawingRect_);

    // Clear the rectangle.
    fillPaint_.setColor(Color.WHITE);
    canvas.drawRect(drawingRect_, fillPaint_);
    strokePaint_.setStrokeWidth(1.0f);

    // Draw piano keys to mark the frequencies.
    for (int note = (int)minNote_; note < (int)maxNote_; ++note) {
      // Draw a single key that fills up a row.
      keyRect_.bottom = getNoteY(note);
      keyRect_.top = getNoteY(note + 1);
      keyRect_.left = getTimeX(0.0);
      keyRect_.right = getTimeX(maxTime_);
      strokePaint_.setStrokeWidth(2.0f);
      strokePaint_.setColor(Color.LTGRAY);
      if (Note.isNatural(note)) {
        fillPaint_.setColor(Color.WHITE);
      } else {
        fillPaint_.setColor(Color.LTGRAY);
      }
      canvas.drawRect(keyRect_, fillPaint_);
      canvas.drawRect(keyRect_, strokePaint_);

      if (currentTool_ != null) {
        currentTool_.afterDrawKey(note, canvas, keyRect_);
      }
    }

    // Draw lines to mark the measures.
    for (double i = minTime_ + 1; i < maxTime_; ++i) {
      strokePaint_.setColor(Color.LTGRAY);
      int x = getTimeX(i - 0.75);
      canvas.drawLine(x, drawingRect_.top, x, drawingRect_.bottom, strokePaint_);

      strokePaint_.setColor(Color.GRAY);
      x = getTimeX(i - 0.5);
      canvas.drawLine(x, drawingRect_.top, x, drawingRect_.bottom, strokePaint_);

      strokePaint_.setColor(Color.LTGRAY);
      x = getTimeX(i - 0.25);
      canvas.drawLine(x, drawingRect_.top, x, drawingRect_.bottom, strokePaint_);

      strokePaint_.setColor(Color.BLACK);
      x = getTimeX(i);
      canvas.drawLine(x, drawingRect_.top, x, drawingRect_.bottom, strokePaint_);
    }

    // Draw the margins.
    double leftMargin = getTimeX(minTime_);
    if (leftMargin > drawingRect_.left) {
      canvas.drawRect(drawingRect_.left,
                      drawingRect_.top,
                      (float)leftMargin,
                      drawingRect_.bottom,
                      marginPaint_);
    }
    double rightMargin = getTimeX(maxTime_);
    if (rightMargin < drawingRect_.right) {
      canvas.drawRect((float)rightMargin,
                      drawingRect_.top,
                      drawingRect_.right,
                      drawingRect_.bottom,
                      marginPaint_);
    }
    double topMargin = getNoteY(maxNote_);
    if (topMargin > drawingRect_.top) {
      canvas.drawRect(drawingRect_.left,
                      drawingRect_.top,
                      drawingRect_.right,
                      (float)topMargin,
                      marginPaint_);
    }
    double bottomMargin = getNoteY(minNote_);
    if (bottomMargin < drawingRect_.bottom) {
      canvas.drawRect(drawingRect_.left,
                      (float)bottomMargin,
                      drawingRect_.right,
                      drawingRect_.bottom,
                      marginPaint_);
    }

    // Draw the sequence.
    for (int i = 0; i < score_.getEventCount(); ++i) {
      Event event = score_.getEvent(i);
      eventRect_.left = getTimeX(event.getStart());
      eventRect_.top = getNoteY(event.getKey() + 1);
      eventRect_.right = getTimeX(event.getEnd());
      eventRect_.bottom = getNoteY(event.getKey());

      if (!event.hasKeyEvent() || isChannelVisible(event.getKeyEvent().getChannel())) {
        if (event.getSelected()) {
          if (event.hasKeyEvent()) {
            fillPaint_.setColor(getColorForChannel(event.getKeyEvent().getChannel()));
            strokePaint_.setColor(Color.BLACK);
          } else {
            fillPaint_.setColor(Color.BLACK);
            strokePaint_.setColor(Color.WHITE);
          }
          fillPaint_.setAlpha(255);
          strokePaint_.setStrokeWidth(5.0f);
        } else {
          if (event.hasKeyEvent()) {
            fillPaint_.setColor(getColorForChannel(event.getKeyEvent().getChannel()));
            strokePaint_.setColor(Color.BLACK);
          } else {
            fillPaint_.setColor(Color.BLACK);
            strokePaint_.setColor(Color.WHITE);
          }
          fillPaint_.setAlpha(127);
          strokePaint_.setStrokeWidth(1.0f);
        }
        canvas.drawRect(eventRect_, fillPaint_);
        canvas.drawRect(eventRect_, strokePaint_);

        if (currentTool_ != null) {
          currentTool_.afterDrawEvent(event, canvas, eventRect_);
        }
      }
    }

    // Draw the cursor.
    strokePaint_.setColor(Color.rgb(0, 175, 0));
    strokePaint_.setStrokeWidth(8.0f);
    canvas.drawLine(getTimeX(cursor_), drawingRect_.top,
                    getTimeX(cursor_), drawingRect_.bottom, strokePaint_);

    // Draw the scroll arrows, if visible.
    if (arrowsVisible_) {
      upIcon_.setBounds(getDrawingRect().left + 50,
                        getDrawingRect().top + 50,
                        getDrawingRect().left + 50 + upIcon_.getIntrinsicWidth(),
                        getDrawingRect().top + 50 + downIcon_.getIntrinsicHeight());
      downIcon_.setBounds(getDrawingRect().left + 50,
                          (getDrawingRect().bottom - 50) - downIcon_.getIntrinsicHeight(),
                          getDrawingRect().left + 50 + upIcon_.getIntrinsicWidth(),
                          getDrawingRect().bottom - 50);

      if (upSelected_) {
        fillPaint_.setColor(Color.WHITE);
      } else {
        fillPaint_.setColor(Color.BLACK);
      }
      canvas.drawCircle(upIcon_.getBounds().centerX(),
                        upIcon_.getBounds().centerY(),
                        upIcon_.getBounds().width(),
                        fillPaint_);
      upIcon_.draw(canvas);

      if (downSelected_) {
        fillPaint_.setColor(Color.WHITE);
      } else {
        fillPaint_.setColor(Color.BLACK);
      }
      canvas.drawCircle(downIcon_.getBounds().centerX(),
                        downIcon_.getBounds().centerY(),
                        downIcon_.getBounds().width(),
                        fillPaint_);
      downIcon_.draw(canvas);

      // Make the bounds for the icons a little larger so they're easier to hit.
      downIcon_.getBounds().inset(-50, -50);
      upIcon_.getBounds().inset(-50, -50);
    }

    if (currentTool_ != null) {
      currentTool_.afterDrawScore(this, canvas, drawingRect_);
    }
  }

  /**
   * Handler for all touch events.
   */
  @Override
  public boolean onTouchEvent(MotionEvent event) {
    // Check if they touched the arrows.
    int action = event.getAction();
    int actionCode = action & MotionEvent.ACTION_MASK;
    if (actionCode == MotionEvent.ACTION_DOWN) {
      if (upIcon_.getBounds().contains((int)event.getX(), (int)event.getY())) {
        upSelected_ = true;
        invalidate();
        return true;
      } else if (downIcon_.getBounds().contains((int)event.getX(), (int)event.getY())) {
        downSelected_ = true;
        invalidate();
        return true;
      } else {
        arrowsVisible_ = false;
        invalidate();
      }
    } else if (actionCode == MotionEvent.ACTION_UP) {
      arrowsVisible_ = true;
      if (upSelected_) {
        // Scroll up.
        if (getNoteOffset() < maxNote_) {
          setNoteOffset(getNoteOffset() + 1);
        }
        upSelected_ = false;
      }
      if (downSelected_) {
        // Scroll down.
        if (getNoteOffset() > minNote_) {
          setNoteOffset(getNoteOffset() - 1);
        }
        downSelected_ = false;
      }
      invalidate();
    }

    // Delegate the touch to the current tool.
    if (!upSelected_ && !downSelected_ && currentTool_ != null) {
      return currentTool_.onTouch(this, event);
    } else {
      return false;
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

  /**
   * Connects the ScoreView to a Synthesizer for playback.
   * @synth - The synthesizer to connect to.
   */
  public void bindTo(final MidiListener synth) {
    synthesizer_ = synth;
  }

  /**
   * Returns the synthesizer connected to this ScoreView.
   * @return the connected synthesizer.
   */
  public MidiListener getSynthesizer() {
    return synthesizer_;
  }

  /**
   * Sets the listener to notify of events in this control.
   * @param listener - the listener to notify.
   */
  public void setListener(ScoreViewListener listener) {
    listener_ = listener;
  }

  // The score being edited, played, etc by this control.
  private Score.Builder score_;

  // The current tool being used.
  private ScoreViewTool currentTool_;

  // The currently selected channel (instrument).
  private int currentChannel_;

  // Whether to show channels other than the currently selected one.
  private boolean showOtherChannels_;

  // The set of icons to use for each channel.
  private Drawable[] iconForChannel_;

  // What granularity of note to snap to when editing.  See getSnapTo and setSnapTo().
  private double snapTo_;

  // The min, max and current viewport for the x and y axes.
  private double timeZoom_;
  private double timeOffset_;
  private double minTime_;
  private double maxTime_;
  private double noteZoom_;
  private double noteOffset_;
  private double minNote_;
  private double maxNote_;

  // A cursor that indicates where playback is in the score, in logical coordinates.
  private double cursor_;

  // The synthesizer this control is bound to.
  private MidiListener synthesizer_;

  // The listener to notify of events in this control.
  private ScoreViewListener listener_;

  // Buttons to let the user move up and down without switching to the viewport tool.
  private boolean arrowsVisible_;
  private boolean upSelected_;
  private boolean downSelected_;
  private Drawable upIcon_;
  private Drawable downIcon_;

  // These are basically stack variables for onDraw.  They're member variables only so that we can
  // avoid reallocating them every time the keyboard is redrawn.
  //
  // The most recent screen rect that this keyboard was drawn into.
  private Rect drawingRect_;
  private Rect keyRect_;
  private Rect eventRect_;
  private Paint fillPaint_;
  private Paint strokePaint_;
  private Paint marginPaint_;

  // The number of channels (instruments) edittable by this control.
  private static final int CHANNELS = 5;

  @SuppressWarnings("unused")
  private Logger logger_;
}
