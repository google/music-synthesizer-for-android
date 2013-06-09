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

import java.io.IOException;

import com.levien.synthesizer.core.wave.RiffInputStream;

/**
 * A Bag is a structure particular to the SoundFont format that represents a list of generators and
 * modulators.  It could be a "preset bag" or an "instrument bag", but it's basically the same.
 */
public class Bag {
  /**
   * Reads one Bag from the given input stream.
   * @param input - An input stream where the next bytes represent a bag.
   */
  public Bag(RiffInputStream input) throws IOException {
    generatorStart_ = input.readWord();
    modulatorStart_ = input.readWord();
    generatorEnd_ = generatorStart_;
    modulatorEnd_ = modulatorStart_;
  }

  /**
   * @return The index of the first generator in this bag.
   */
  public int getGeneratorStart() {
    return generatorStart_;
  }

  /**
   * @return The index of the first modulator in this bag.
   */
  public int getModulatorStart() {
    return modulatorStart_;
  }

  /**
   * @return The index of the first generator after getGeneratorStart() *not* in this bag.
   */
  public int getGeneratorEnd() {
    return generatorEnd_;
  }

  /**
   * @return The index of the first modulator after getModulatorStart() *not* in this bag.
   */
  public int getModulatorEnd() {
    return modulatorEnd_;
  }

  /**
   * Sets the value to be returned by getGeneratorEnd().
   */
  public void setGeneratorEnd(int end) {
    generatorEnd_ = end;
  }

  /**
   * Sets the value to be returned by getModulatorEnd().
   */
  public void setModulatorEnd(int end) {
    modulatorEnd_ = end;
  }

  /**
   * Returns a user-readable string representing this bag.
   */
  public String toString() {
    return
        "Bag {\n" +
        "  generator: [" + generatorStart_ + ", " + generatorEnd_ + ")\n" +
        "  modulator: [" + modulatorStart_ + ", " + modulatorEnd_ + ")\n" +
        "}";
  }

  private int generatorStart_;
  private int modulatorStart_;
  private int generatorEnd_;
  private int modulatorEnd_;
}
