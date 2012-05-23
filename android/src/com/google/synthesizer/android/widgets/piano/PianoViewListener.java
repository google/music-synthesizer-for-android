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

// Simple interface for listening to piano widget events.
public interface PianoViewListener {
  /**
   * A note was pressed.
   * @param logFrequency - the log frequency of the note pressed.
   * @param retriggerIfOn - true if this is a new touch, rather than just moving.
   */
  void noteDown(double logFrequency, int finger, boolean retriggerIfOn, float pressure);
  
  /**
   * The note was released.
   */
  void noteUp(int finger);
}