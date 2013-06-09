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

package com.levien.synthesizer.android;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Environment;
import android.widget.EditText;
import android.widget.Toast;

import com.levien.synthesizer.core.music.Music.Score;

/**
 * A collection of functions for storing and retrieving scores.
 */
public class Storage {
  /**
   * Opens the score stored with the name "_default", which should always be the current score
   * being edited.  This may be called at any time to restore saved state, since Android Activities
   * can come and go.
   * 
   * @param score - The mutable score to populate with the stored data.
   * @param context - Android application context.
   */
  public static void openDefaultScore(Score.Builder score, Context context) throws IOException {
    openScore(score, "_default", context);
  }

  /**
   * Saves a score with the name "_default", which should always be the current score being edited.
   * This may be called at any time to save state, since Android Activities can come and go.
   * 
   * @param score - The score data to save.
   * @param context - Android application context.
   */
  public static void saveDefaultScore(Score score, Context context) throws IOException {
    saveScore(score, "_default", true, context);
  }

  /**
   * Opens the score with the given name.  The name should be name of a valid score file in storage.
   * The file must be in the root external files directory for the app.
   * 
   * @param score - The mutable score to update with the data from storage.
   * @param name - The name of the file, minus the ".pb" extension.
   * @param context - The Android application context.
   */
  public static void openScore(Score.Builder score, String name, Context context) throws IOException {
    if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) &&
        !Environment.MEDIA_MOUNTED_READ_ONLY.equals(Environment.getExternalStorageState())) {
      throw new IOException("External storage is not readable.");
    }
    File path = context.getExternalFilesDir(null);
    File file = new File(path, name + ".pb");
    FileInputStream in = new FileInputStream(file);
    score.clear();
    score.mergeFrom(in);
    in.close();
  }

  /**
   * Saves the score with the given name.  Files are stored in the root external files directory for
   * the app.
   * 
   * @param score - The score to save.
   * @param name - The name of the file, without any extension.
   * @param overwrite - If true, replace the existing file, if one already exists.
   * @param context - The Android application context.
   * @throws IOException - On any kind of IO error, or if name is "", or the file already exists.
   */
  public static void saveScore(Score score,
                               String name,
                               boolean overwrite,
                               Context context) throws IOException {
    if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
      throw new IOException("External storage is not writeable.");
    }
    File path = context.getExternalFilesDir(null);
    name = cleanupName(name);
    if (name.length() == 0) {
      throw new IOException("Can't save score without a name.");
    }    
    File file = new File(path, name + ".pb");
    if (!overwrite && file.exists()) {
      throw new IOException("File already exists.");
    }
    FileOutputStream out = new FileOutputStream(file);
    score.writeTo(out);
    out.close();
  }

  /**
   * Returns the list of all valid names of scores that are currently in storage.
   */
  public static String[] getScoreNames(Context context) throws IOException {
    if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) &&
        !Environment.MEDIA_MOUNTED_READ_ONLY.equals(Environment.getExternalStorageState())) {
      throw new IOException("External storage is not readable.");
    }
    File path = context.getExternalFilesDir(null);
    File[] files = path.listFiles();
    ArrayList<String> names = new ArrayList<String>();
    for (File file : files) {
      names.add(file.getName().replaceAll("\\.pb", ""));
    }
    return names.toArray(new String[0]);
  }

  /**
   * Returns true iff there is a score in storage with the given name.
   */
  public static boolean scoreExists(String name, Context context) throws IOException {
    String[] names = getScoreNames(context);
    for (String existingName : names) {
      if (name.equals(existingName)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Interface used to notify callers of openScoreWithDialog() when the opening has completed.
   */
  public interface OpenScoreListener {
    void onOpenScore(Score.Builder score);
  }

  /**
   * Shows UI to allow the user to pick one of the current scores in storage, and then populates
   * score with the data from that file.
   *
   * @param score - The mutable score to update.
   * @param listener - A listener that is notified after the score is updated.  Can be null.
   * @param context - An Android application context.
   */
  public static void openScoreWithDialog(final Score.Builder score,
                                         final OpenScoreListener listener,
                                         final Context context) {
    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder.setTitle("Open score...");
    try {
      final String[] scoreNames = getScoreNames(context);
      builder.setItems(scoreNames, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
          try {
            openScore(score, scoreNames[which], context);
            if (listener != null) {
              listener.onOpenScore(score);
            }
            dialog.dismiss();
          } catch (IOException e) {
            Logger logger = Logger.getLogger(Storage.class.getName());
            logger.log(Level.SEVERE,
                       "Error opening score \"" + scoreNames[which] + "\" with dialog.", e);
            Toast.makeText(context,
                           "Unable to open \"" + scoreNames[which] + "\".",
                           Toast.LENGTH_SHORT).show();
          }
        }
      });
    } catch (IOException e) {
      Logger logger = Logger.getLogger(Storage.class.getName());
      logger.log(Level.SEVERE,
                 "Error getting score names.", e);
      Toast.makeText(context,
                     "Unable to get existing score names.",
                     Toast.LENGTH_SHORT).show();
    }
    AlertDialog dialog = builder.create();
    dialog.show();
  }

  /**
   * Shows UI to allow the user to pick a name and save the given score in storage.
   *
   * @param score - The score to save.
   * @param context - An Android application context.
   */
  public static void saveScoreWithDialog(final Score score,
                                         final Context context) {
    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder.setTitle("Save score as...");
    builder.setMessage("Name: ");
    final EditText input = new EditText(context);
    builder.setView(input);
    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
      public void onClick(final DialogInterface nameDialog, int which) {
        final String name = cleanupName(input.getText().toString());
        if (name.length() == 0) {
          Toast.makeText(context,
                         "Name must not be empty.",
                         Toast.LENGTH_SHORT).show();
        } else {
          try {
            if (scoreExists(name, context)) {
              AlertDialog.Builder builder = new AlertDialog.Builder(context);
              builder.setTitle("Overwrite?");
              builder.setMessage(
                  "A score named " + name + " already exists.  Would you like to overwrite it?");
              builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface confirmDialog, int which) {
                  try {
                    saveScore(score, name, true, context);
                    confirmDialog.dismiss();
                    nameDialog.dismiss();
                  } catch (IOException e) {
                    Logger logger = Logger.getLogger(Storage.class.getName());
                    logger.log(Level.SEVERE,
                               "Error saving score \"" + name + "\" with dialog.", e);
                    Toast.makeText(context,
                                   "Unable to save \"" + name + "\".",
                                   Toast.LENGTH_SHORT).show();
                  }
                }
              });
              builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                  dialog.dismiss();
                }
              });
              AlertDialog confirmDialog = builder.create();
              confirmDialog.show();
            } else {
              saveScore(score, name, false, context);
              nameDialog.dismiss();
            }
          } catch (IOException e) {
            Logger logger = Logger.getLogger(Storage.class.getName());
            logger.log(Level.SEVERE,
                       "Error saving score \"" + name + "\" with dialog.", e);
            Toast.makeText(context,
                           "Unable to save \"" + name + "\".",
                           Toast.LENGTH_SHORT).show();
          }
        }
      }
    });
    builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
        dialog.dismiss();
      }
    });
    AlertDialog dialog = builder.create();
    dialog.show();
  }

  /**
   * Internal method to turn a user input string into a valid file name.
   */
  private static String cleanupName(String name) {
    name = name.trim();
    name = name.replaceAll("[^A-Za-z0-9_-]", "_");
    return name;
  }
}
