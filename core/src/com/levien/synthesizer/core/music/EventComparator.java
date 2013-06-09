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

package com.levien.synthesizer.core.music;

import java.util.Comparator;

import com.levien.synthesizer.core.music.Music.EventOrBuilder;

/**
 * A few comparators for sorting lists of Events (like in a score).
 */
public class EventComparator {
  /**
   * Sorts by the start times of the events.
   */
  public static Comparator<EventOrBuilder> byStart() {
    return new Comparator<EventOrBuilder>() {
      public int compare(EventOrBuilder event, EventOrBuilder other) {
        if (other.getStart() == event.getStart()) {
          if (other.hasKeyEvent() && !event.hasKeyEvent()) {
            return -1;
          } else if (!other.hasKeyEvent() && event.hasKeyEvent()) {
            return 1;
          } else if (other.getEnd() == event.getEnd()) {
            return 0;
          } else if (other.getEnd() > event.getEnd()) {
            return 1;
          } else {
            return -1;
          }
        } else if (other.getStart() < event.getStart()) {
          return 1;
        } else {
          return -1;
        }
      }
    };
  }

  /**
   * Sorts by the end times of the events.
   */
  public static Comparator<EventOrBuilder> byEnd() {
    return new Comparator<EventOrBuilder>() {
      public int compare(EventOrBuilder event, EventOrBuilder other) {
        if (other.getEnd() == event.getEnd()) {
          if (other.hasKeyEvent() && !event.hasKeyEvent()) {
            return 1;
          } else if (!other.hasKeyEvent() && event.hasKeyEvent()) {
            return -1;
          } else if (other.getStart() == event.getStart()) {
            return 0;
          } else if (other.getStart() < event.getStart()) {
            return -1;
          } else {
            return 1;
          }
        } else if (other.getEnd() < event.getEnd()) {
          return 1;
        } else {
          return -1;
        }
      }
    };
  }
}
