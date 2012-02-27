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

import com.google.synthesizer.core.wave.RiffInputStream;

/**
 * A Modulator is a SoundFont data structure.  They are read in by this library, but then ignored.
 */
public class Modulator {
  public Modulator(RiffInputStream input) throws IOException {
    sourceOperator = new ModulatorSource(input.readWord());
    destinationOperator = input.readWord();
    amount = input.readShort();
    amountOperator = new ModulatorSource(input.readWord());
    transform = input.readWord();
  }

  public String toString() {
    return
        "Modulator {\n" +
        "  source operator: \n" +
        sourceOperator + "\n" +
        "  destination operator: " + destinationOperator + "\n" +
        "  amount: " + amount + "\n" +
        "  amount operator: \n" +
        amountOperator + "\n" +
        "  transform: " + transform + "\n" +
        "}";
  }

  public ModulatorSource sourceOperator;
  public int destinationOperator;
  public short amount;
  public ModulatorSource amountOperator;
  public int transform;
}