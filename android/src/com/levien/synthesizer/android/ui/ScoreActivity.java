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

package com.levien.synthesizer.android.ui;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.levien.synthesizer.R;
import com.levien.synthesizer.android.Storage;
import com.levien.synthesizer.android.widgets.score.ScoreView;
import com.levien.synthesizer.android.widgets.score.ScoreViewToolbar;
import com.levien.synthesizer.core.midi.MidiListener;
import com.levien.synthesizer.core.music.Music.Score.Builder;

/**
 * An Activity for editing or playing a score.
 */
public class ScoreActivity extends SynthActivity {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.score);

    logger_ = Logger.getLogger(getClass().getName());

    scoreView_ = (ScoreView)findViewById(R.id.score);
    scoreViewToolbar_ = (ScoreViewToolbar)findViewById(R.id.toolbar);

    scoreViewToolbar_.setScoreView(scoreView_);
  }

  @Override
  protected void onStart() {
    super.onStart();
    try {
      Storage.openDefaultScore(scoreView_.getScore(), this.getApplicationContext());
      scoreView_.invalidate();
      scoreViewToolbar_.invalidate();
    } catch (IOException e) {
      logger_.log(Level.SEVERE, "Unable to open score.", e);
    }
  }

  @Override
  protected void onStop() {
    try {
      Storage.saveDefaultScore(scoreView_.getScore().build(), this.getApplicationContext());
    } catch (IOException e) {
      logger_.log(Level.SEVERE, "Unable to save score.", e);
    }
    super.onStop();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.score_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.new_score:
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("New score...");
        builder.setMessage("This will erase any unsaved work.  Are you sure?");
        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            scoreView_.getScore().clear();
            scoreView_.invalidate();
            scoreViewToolbar_.invalidate();
            dialog.dismiss();
          }
        });
        builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
          }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
        return true;
      case R.id.open_score:
        Storage.openScoreWithDialog(scoreView_.getScore(), new Storage.OpenScoreListener() {
          public void onOpenScore(Builder score) {
            scoreView_.invalidate();
            scoreViewToolbar_.invalidate();
          }
        }, this);
        return true;
      case R.id.save_score:
        Storage.saveScoreWithDialog(scoreView_.getScore().build(), this);
        return true;
      case R.id.piano:
        this.startActivity(new Intent(this, PianoActivity.class));
        return true;
      case R.id.chord_grid:
        this.startActivity(new Intent(this, ChordGridActivity.class));
        return true;
      case R.id.edit_instrument:
        this.startActivity(new Intent(this, InstrumentListActivity.class));
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  protected void onSynthConnected() {
    final MidiListener synthMidi = synthesizerService_.getMidiListener();
    scoreView_.bindTo(synthMidi);
  }

  private ScoreView scoreView_;
  private ScoreViewToolbar scoreViewToolbar_;

  private Logger logger_;
}
