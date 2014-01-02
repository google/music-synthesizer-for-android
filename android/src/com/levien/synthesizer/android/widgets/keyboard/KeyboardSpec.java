/*
 * Copyright 2013 Google Inc.
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

package com.levien.synthesizer.android.widgets.keyboard;

import android.graphics.Color;
import android.graphics.RectF;

public class KeyboardSpec {
  public KeyboardSpec(int nkeys, float repeatWidth, float height) {
    keys = new KeySpec[nkeys];
    this.repeatWidth = repeatWidth;
    this.height = height;
  }

  public void addKey(KeySpec key) {
    keys[ix_++] = key;
    this.maxX = Math.max(this.maxX, key.rect.right);
  }

  public void addKey(float x0, float y0, float w, float h, int color) {
    KeySpec ks = new KeySpec();
    ks.rect = new RectF(x0, y0, x0 + w, y0 + h);
    ks.color = color;
    addKey(ks);
  }

  public static KeyboardSpec make2Row() {
    KeyboardSpec ks = new KeyboardSpec(12, 84, 24);
    ks.addKey(0, 12, 12, 12, W);   // C
    ks.addKey(6, 0, 12, 12, B);    // C#
    ks.addKey(12, 12, 12, 12, W);  // D
    ks.addKey(18, 0, 12, 12, B);   // D#
    ks.addKey(24, 12, 12, 12, W);  // E
    ks.addKey(36, 12, 12, 12, W);  // F
    ks.addKey(42, 0, 12, 12, B);   // F#
    ks.addKey(48, 12, 12, 12, W);  // G
    ks.addKey(54, 0, 12, 12, B);   // G#
    ks.addKey(60, 12, 12, 12, W);  // A
    ks.addKey(66, 0, 12, 12, B);   // A#
    ks.addKey(72, 12, 12, 12, W);  // B
    return ks;
  }

  public static KeyboardSpec make3Row() {
    KeyboardSpec ks = new KeyboardSpec(24, 84, 32);
    for (int oct = 0; oct < 2; oct++) {
      float x0 = 42 * oct;
      float y1 = 20 - 12 * oct;
      float y2 = 28 - y1;
      ks.addKey(x0 + 0, y1, 12, 12, W);   // C
      ks.addKey(x0 + 4, 0, 8, 8, B);      // C#
      ks.addKey(x0 + 6, y2, 12, 12, W);   // D
      ks.addKey(x0 + 12, 0, 8, 8, B);     // D#
      ks.addKey(x0 + 12, y1, 12, 12, W);  // E
      ks.addKey(x0 + 18, y2, 12, 12, W);  // F
      ks.addKey(x0 + 21, 0, 8, 8, B);     // F#
      ks.addKey(x0 + 24, y1, 12, 12, W);  // G
      ks.addKey(x0 + 29, 0, 8, 8, B);     // G#
      ks.addKey(x0 + 30, y2, 12, 12, W);  // A
      ks.addKey(x0 + 37, 0, 8, 8, B);     // A#
      ks.addKey(x0 + 36, y1, 12, 12, W);  // B
    }
    return ks;
  }

  public static KeyboardSpec make3RowChromatic() {
    KeyboardSpec ks = new KeyboardSpec(12, 12, 9);
    ks.addKey(0, 6, 3, 3, W);   // C
    ks.addKey(1, 3, 3, 3, B);   // C#
    ks.addKey(2, 0, 3, 3, W);   // D
    ks.addKey(3, 6, 3, 3, B);   // D#
    ks.addKey(4, 3, 3, 3, W);   // E
    ks.addKey(5, 0, 3, 3, W);   // F
    ks.addKey(6, 6, 3, 3, B);   // F#
    ks.addKey(7, 3, 3, 3, W);   // G
    ks.addKey(8, 0, 3, 3, B);   // G#
    ks.addKey(9, 6, 3, 3, W);   // A
    ks.addKey(10, 3, 3, 3, B);  // A#
    ks.addKey(11, 0, 3, 3, W);  // B
    return ks;
  }

  public static KeyboardSpec make(String name) {
    if ("2row".equals(name)) {
      return make2Row();
    } else if ("3row".equals(name)) {
      return make3Row();
    } else if ("3chrome".equals(name)) {
      return make3RowChromatic();
    } else {
      return null;
    }
  }

  public KeySpec keys[];
  public float repeatWidth;
  public float height;
  public float maxX;
  private int ix_;

  private static final int B = Color.GRAY;
  private static final int W = Color.WHITE;
}
