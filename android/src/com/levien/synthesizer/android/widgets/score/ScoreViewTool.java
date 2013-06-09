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

import com.levien.synthesizer.core.music.Music.Event;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.MotionEvent;

/**
 * A base class for tools on the ScoreView's toolbar.
 */
public abstract class ScoreViewTool {
  /**
   * Draws the button on the toolbar.
   * @param canvas - The canvas to draw the button on.
   * @param score - The ScoreView that this tool is for.
   * @param rect - The area of the button to be drawn, including any margin.
   * @param margin - The preferred margin around the button, in screen coordinates.
   */
  public abstract void drawButton(Canvas canvas, ScoreView score, Rect rect, float margin);

  /**
   * Called when this tool is selected.
   * @param view - The ScoreView that this tool is for.
   * @param previousTool - The tool that was selected when this one was chosen.
   */
  public void onSelect(ScoreView view, ScoreViewTool previousTool) {}

  /**
   * Called when the user touches the ScoreView while this tool is selected.
   * @param view - The ScoreView that this tool is for.
   * @param event - The touch event that triggered this handler.
   * @return true iff this tool handled the touch event.
   */
  public boolean onTouch(ScoreView view, MotionEvent event) {
    return false;
  }

  /**
   * Called after each key is drawn, to give this tool a chance to draw over it.
   * See ScoreView.onDraw() for more information on how ScoreView is drawn.
   * @param key - The key that was drawn.
   * @param canvas - The canvas the key is drawn into.
   * @param rect - The area of the key on the canvas.
   */
  public void afterDrawKey(int key, Canvas canvas, Rect rect) {}

  /**
   * Called after each event is drawn, to give this tool a chance to draw over it.
   * See ScoreView.onDraw() for more information on how ScoreView is drawn.
   * @param event - The event that was drawn.
   * @param canvas - The canvas the key is drawn into.
   * @param rect - The area of the key on the canvas.
   */
  public void afterDrawEvent(Event event, Canvas canvas, Rect rect) {}

  /**
   * Called after the entire score is drawn, to give this tool a chance to draw over it.
   * See ScoreView.onDraw() for more information on how ScoreView is drawn.
   * @param view - The ScoreView being drawn.
   * @param canvas - The canvas the key is drawn into.
   * @param rect - The area of the key on the canvas.
   */
  public void afterDrawScore(ScoreView view, Canvas canvas, Rect rect) {}
}
