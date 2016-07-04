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

package com.levien.synthesizer.core.midi;

/**
 * A MidiEvent is the combination of a midi message and a time offset.  The message is represented
 * as an opaque array of bytes that can be interpreted using a MessageProcessor.
 * @see MessageInputProcessor
 */
public class MidiEvent {
  /**
   * Creates a new MidiEvent with null values.
   */
  public MidiEvent() {
    deltaTime_ = 0;
    message_ = null;
  }

  /**
   * Creates a new MidiEvent with the given parameters.
   * @param deltaTime - The time in midi "ticks" between the previous event occurring and this one.
   * @param message - The midi message as an opaque array of bytes.
   */
  public MidiEvent(long deltaTime, byte[] message) {
    deltaTime_ = deltaTime;
    message_ = message;
  }

  /**
   * Sets the time offset of the event.
   * @param deltaTime - The time in midi "ticks" between the previous event occurring and this one.
   */
  public void setDeltaTime(long deltaTime) {
    deltaTime_ = deltaTime;
  }

  /**
   * Sets the message of the event.
   * @param message - The midi message as an opaque array of bytes.
   */
  public void setMessage(byte[] message) {
    message_ = message;
  }

  /**
   * Gets the time offset of the event.
   * @return The time in midi "ticks" between the previous event occurring and this one.
   */
  public long getDeltaTime() {
    return deltaTime_;
  }

  /**
   * Gets the message of the event.
   * @return The midi message as an opaque array of bytes, which may be null.
   */
  public byte[] getMessage() {
    return message_;
  }

  // The time in midi "ticks" between the previous event occurring and this one.
  private long deltaTime_;

  // The midi message as an opaque array of bytes, which may be null.
  private byte[] message_;
}
