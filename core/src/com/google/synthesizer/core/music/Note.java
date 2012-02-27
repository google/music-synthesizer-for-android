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

package com.google.synthesizer.core.music;

/**
 * Collection of static methods dealing with musical notes.
 */
public class Note {
  /**
   * Determines the log frequency of a note in 12 tone equal temperament (12-TET).
   * @param note - the note, a value from one of the constants in this class.
   * @param octave - the octave of the note.
   */
  public static double computeLog12TET(int note, int octave) {
    return FACTOR + ((((12 * octave) + (note - 9) + 1) - 49) / 12.0);
  }

  /**
   * Returns the note, or midi key, that corresponds to the given log frequency.
   * @param logFrequency - the log frequency that corresponds to a note.
   * @return - the nearest note to that frequency.
   */
  public static int getKeyforLog12TET(double logFrequency) {
    return (int)(12.0 * (logFrequency - FACTOR) + 57.5);
  }

  /**
   * Returns true if the note is natural, i.e. not a sharp or flat.
   * @param note - the note, a value from one of the constants in this class.
   */
  public static boolean isNatural(int note) {
    return NATURAL[note % NATURAL.length];
  }

  /**
   * Returns a printable name for the given note.
   * @param note - the note, a value from one of the constants in this class.
   */
  public static String getName(int note) {
    return NAMES[note];
  }

  // Note frequencies in 12-TET are defined relative to log2 of A4 (440Hz).
  // There's no reason to redo the log conversion every time a frequency is computed.
  private static final double FACTOR = Math.log(440.0) / Math.log(2.0);

  // Constants for each of the possible notes.
  public static final int NONE = -1;
  public static final int C = 0;
  public static final int C_SHARP = 1;
  public static final int D_FLAT = 1;
  public static final int D = 2;
  public static final int D_SHARP = 3;
  public static final int E_FLAT = 3;
  public static final int E = 4;
  public static final int F = 5;
  public static final int F_SHARP = 6;
  public static final int G_FLAT = 6;
  public static final int G = 7;
  public static final int G_SHARP = 8;
  public static final int A_FLAT = 8;
  public static final int A = 9;
  public static final int A_SHARP = 10;
  public static final int B_FLAT = 10;
  public static final int B = 11;
  
  // Constants for intervals between notes.
  public static final double HALF_STEP = 1.0f / 12.0f;
  public static final double WHOLE_STEP = 1.0f / 6.0f;
  public static final double OCTAVE = 1.0f;

  // A simple map of which notes are natural.
  private static final boolean[] NATURAL = {
      true, false, true, false, true, true, false, true, false, true, false, true };

  // A displayable name for each note.
  private static final String[] NAMES = {
      "C", "C#", "D", "Eb", "E", "F", "F#", "G", "Ab", "A", "Bb", "B"
  };
}
