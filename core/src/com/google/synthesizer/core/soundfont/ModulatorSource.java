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

package com.google.synthesizer.core.soundfont;

/**
 * A ModulatorSource is a SoundFont data structure.  They are read by this library and then ignored.
 */
public class ModulatorSource {
  /**
   * Initializes the source from the given value.
   */
  public ModulatorSource(int value) {
    type_ = (byte)((0xFC00 & value) >> 10);
    polarity_ = ((value & 0x0200) != 0);
    direction_ = ((value & 0x0100) != 0);
    continuous_ = ((value & 0x0080) != 0);
    index_ = (byte)(0x007F & value);
  }

  public String toString() {
    return
        "    ModulatorSource {\n" +
        "      type: " + type_ + "\n" +
        "      polarity: " + polarity_ + "\n" +
        "      direction: " + direction_ + "\n" +
        "      continuous: " + continuous_ + "\n" +
        "      index: " + index_ + "\n" +
        "    }";
  }

  private byte type_;
  public boolean polarity_;
  public boolean direction_;
  public boolean continuous_;
  public byte index_;
}
