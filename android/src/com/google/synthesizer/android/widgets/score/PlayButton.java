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

package com.google.synthesizer.android.widgets.score;

import java.util.logging.Logger;

import com.google.synthesizer.R;
import com.google.synthesizer.core.music.ScorePlayer;
import com.google.synthesizer.core.music.ScorePlayerListener;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/**
 * A button to start the current score playing.
 */
public class PlayButton extends ScoreViewTool implements ScorePlayerListener {
  /**
   * Creates a new play button, loading resources from the given context.
   */
  PlayButton(ScoreViewToolbar toolbar, Context context) {
    toolbar_ = toolbar;
    logger_ = Logger.getLogger(getClass().getName());
    player_ = new ScorePlayer();

    playing_ = false;

    playingIcon_ = context.getResources().getDrawable(R.drawable.stop);
    stoppedIcon_ = context.getResources().getDrawable(R.drawable.play);
    paint_ = new Paint();
  }

  /**
   * Called when this tool is selected.  Starts the score playing.
   * @param view - The ScoreView that this toolbar is for.
   * @param previousTool - The tool that was selected when this one was chosen.
   */
  @Override
  public void onSelect(ScoreView view, ScoreViewTool previousTool) {
    view_ = view;
    if (playing_) {
      player_.stopPlaying();
    } else {
      player_.startPlaying(view.getSynthesizer(), view.getScore().build(), 120.0, 4, this);
    }
    view.setTool(previousTool);
  }

  /**
   * Draws the button on the toolbar.
   * @param canvas - The canvas to draw the button on.
   * @param score - The ScoreView that this toolbar is for.
   * @param rect - The area of the button to be drawn, including any margin.
   * @param margin - The preferred margin around the button, in screen coordinates.
   */
  @Override
  public void drawButton(Canvas canvas, ScoreView score, Rect rect, float margin) {
    if (score.getTool() == this) {
      paint_.setColor(Color.WHITE);
      paint_.setStyle(Paint.Style.FILL);
      canvas.drawRect(rect.left - margin / 2,
                      rect.top - margin / 2,
                      rect.right + margin / 2,
                      rect.bottom + margin / 2,
                      paint_);
    }

    paint_.setColor(Color.BLACK);
    paint_.setStyle(Paint.Style.FILL);
    canvas.drawRect(rect, paint_);

    if (playing_) {
      playingIcon_.setBounds(rect);
      playingIcon_.draw(canvas);
    } else {
      stoppedIcon_.setBounds(rect);
      stoppedIcon_.draw(canvas);
    }
  }

  /**
   * Called when the score starts playing.
   */
  public void onStart() {
    view_.post(new Thread("PlayButton.onStart()") {
      public void run() {
        playing_ = true;
        view_.invalidate();
        toolbar_.invalidate();
      }
    });
  }

  /**
   * Called every so often during playback.
   * @param time - the time in measures from the start of the song.
   */
  public void onTimeUpdate(final double time) {
    view_.post(new Thread("PlayButton.onTimeUpdate()") {
      public void run() {
        view_.setCursor(time);
        view_.invalidate();
        toolbar_.invalidate();
      }
    });
  }

  /**
   * Called when the score stops playing.
   */
  public void onStop() {
    view_.post(new Thread("PlayButton.onStop()") {
      public void run() {
        playing_ = false;
        view_.invalidate();
      }
    });
  }

  // The ScoreView that this button controls.
  private ScoreView view_;
  private ScoreViewToolbar toolbar_;

  // ScorePlayer to play the score.
  private ScorePlayer player_;

  // Is the score playing?
  private boolean playing_;

  // Some objects used in drawing.  They are owned here so that they don't have to be reallocated
  // and garbage collected for every pass of drawing.
  private Paint paint_;
  private Drawable playingIcon_;
  private Drawable stoppedIcon_;

  @SuppressWarnings("unused")
  private Logger logger_;
}
