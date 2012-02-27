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

import java.io.IOException;
import java.util.ArrayList;

import com.google.synthesizer.core.wave.RiffInputStream;

/**
 * A Preset is a SoundFont data structure that corresponds to a bunch of "preset zones" and a set
 * of instruments.
 */
public class Preset {
  /**
   * Reads one Preset from the given input stream.
   */
  public Preset(RiffInputStream input) throws IOException {
    name_ = input.readString(20);
    preset_ = input.readWord();
    bank_ = input.readWord();
    bagStart_ = input.readWord();
    bagEnd_ = bagStart_;
    library_ = input.readDWord();
    genre_ = input.readDWord();
    morphology_ = input.readDWord();
    zoneList_ = new ArrayList<Zone>();
  }

  public String getName() {
    return name_;
  }

  public int getBagStart() {
    return bagStart_;
  }

  public int getBagEnd() {
    return bagEnd_;
  }

  public void setBagEnd(int end) {
    bagEnd_ = end;
  }

  public void addZone(Zone zone) {
    zoneList_.add(zone);
  }

  public ArrayList<Zone> getZoneList() {
    return zoneList_;
  }

  public String toString() {
    return
        "Preset {\n" +
        "  name: \"" + name_ + "\"\n" +
        "  preset: " + preset_ + "\n" +
        "  bank: " + bank_ + "\n" +
        "  bag index: [" + bagStart_ + ", " + bagEnd_ + ")\n" +
        "}";
  }

  private String name_;
  private int preset_;
  private int bank_;

  private int bagStart_;
  private int bagEnd_;

  private ArrayList<Zone> zoneList_;

  @SuppressWarnings("unused")
  private long library_;
  @SuppressWarnings("unused")
  private long genre_;
  @SuppressWarnings("unused")
  private long morphology_;
}