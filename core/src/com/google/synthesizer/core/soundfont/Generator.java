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
 * A Generator is a SoundFont data structure that represents an "operator" and an "amount".
 */
public class Generator {
  /**
   * Reads one generator from the given input.
   */
  public Generator(RiffInputStream input) throws IOException {
    operator_ = Operator.fromType(input.readWord());
    amount_ = input.readShort();
  }

  /**
   * @return The operator for this generator.
   */
  public Operator getOperator() {
    return operator_;
  }

  /**
   * @return The unprocessed argument associated with this generator.
   */
  public short getAmount() {
    return amount_;
  }

  /**
   * Returns a user-readable string representing this generator.
   */
  public String toString() {
    return
        "Generator {\n" +
        "  operator: " + operator_.toString() + " (" + operator_.getType() + ")\n" +
        "  amount: " + amount_ + "\n" +
        "}";
  }

  private Operator operator_;
  private short amount_;
}
