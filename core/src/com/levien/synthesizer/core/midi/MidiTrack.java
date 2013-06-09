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

package com.levien.synthesizer.core.midi;

import java.util.ArrayList;
import java.util.List;

/**
 * A MidiTrack is simply a sequence of MidiEvent.
 */
public class MidiTrack {
  /**
   * Creates an empty track.
   */
  public MidiTrack() {
    events_ = new ArrayList<MidiEvent>();
  }

  /**
   * Adds an event to the track.
   */
  public MidiEvent addEvent(MidiEvent event) {
    events_.add(event);
    return event;
  }

  /**
   * Returns the current number of events.
   */
  public int getEventCount() {
    return events_.size();
  }

  /**
   * Gets the event at index i.
   */
  public MidiEvent getEvent(int i) {
    return events_.get(i);
  }

  // The list of events in the track.
  List<MidiEvent> events_;
}
