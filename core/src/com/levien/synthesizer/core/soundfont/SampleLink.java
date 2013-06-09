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

package com.levien.synthesizer.core.soundfont;

/**
 * SampleLink enum for the SoundFont format.
 */
public enum SampleLink {
  UNKNOWN(-1),
  MONO_SAMPLE(1),
  RIGHT_SAMPLE(2),
  LEFT_SAMPLE(4),
  LINKED_SAMPLE(8),
  ROM_MONO_SAMPLE(0x8001),
  ROM_RIGHT_SAMPLE(0x8002),
  ROM_LEFT_SAMPLE(0x8004),
  ROM_LINKED_SAMPLE(0x8008);

  /**
   * Creates a new enum value for the given type.
   */
  SampleLink(int type) {
    type_ = type;
  }

  /**
   * @return The enum value from the SoundFont format.
   */
  public int getType() {
    return type_;
  }

  /**
   * @return The enum corresponding to the given SoundFont format value.
   */
  public static SampleLink fromType(int type) {
    for (SampleLink sl : SampleLink.values()) {
      if (sl.getType() == type) {
        return sl;
      }
    }
    return UNKNOWN;
  }

  private final int type_;
}
