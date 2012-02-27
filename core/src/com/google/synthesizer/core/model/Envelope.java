/*
 * Copyright 2010 Google Inc.
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

package com.google.synthesizer.core.model;

/**
 * An interface for any module that shapes a sound based on something like keyboard keys being
 * pressed and released.
 */
public interface Envelope extends SignalProvider {
  /**
   * Called to tell the envelope that the key has been pressed.
   * @param retriggerIfOn - A (hopefully temporary) hack.  Tells whether to treat this as a new
   *     press if the key is already down.
   */
  public abstract void turnOn(boolean retriggerIfOn);

  /**
   * Called to tell the envelope the key has been released.
   */
  public abstract void turnOff();
}
