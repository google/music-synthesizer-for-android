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
 * An instrument is a SoundFont data structure that consists of a set of zones.  Each "zone" is
 * basically a flattened "bag" with a sample and a key range, etc.
 */
public class Instrument {
  /**
   * Reads one instrument from the given input.
   */
  public Instrument(RiffInputStream input) throws IOException {
    name_ = input.readString(20);
    bagStart_ = input.readWord();
    bagEnd_ = bagStart_;
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

  /**
   * Returns a user-readable string representing this instrument.
   */
  public String toString() {
    return
        "Instrument {\n" +
        "  name: \"" + name_ + "\"\n" +
        "  bag index: [" + bagStart_ + ", " + bagEnd_ + ")\n" +
        "}";
  }

  private String name_;
  private int bagStart_;
  private int bagEnd_;
  private ArrayList<Zone> zoneList_;
}