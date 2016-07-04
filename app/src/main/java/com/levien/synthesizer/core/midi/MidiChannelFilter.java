/*
 * Copyright 2012 Google Inc.
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
 * A MidiChannelFilter is like a MidiListenerProxy, but only passes messages for a given channel.
 * @see MidiListenerProxy
 */
public class MidiChannelFilter extends MidiListenerProxy {
  /**
   * Creates a new MidiChannelFilter that's a proxy for listener that only passes events for the
   * given channel.
   */
  public MidiChannelFilter(MidiListener listener, int channel) {
    super(listener);
    channel_ = channel;
  }

  //
  // The rest of these methods are just midi listener events.
  //

  public void onNoteOff(int channel, int note, int velocity) {
    if (channel == channel_) {
      listener_.onNoteOff(channel, note, velocity);
    }
  }

  public void onNoteOn(int channel, int note, int velocity) {
    if (channel == channel_) {
      listener_.onNoteOn(channel, note, velocity);
    }
  }

  public void onNoteAftertouch(int channel, int note, int aftertouch) {
    if (channel == channel_) {
      listener_.onNoteAftertouch(channel, note, aftertouch);
    }
  }

  public void onController(int channel, int control, int value) {
    if (channel == channel_) {
      listener_.onController(channel, control, value);
    }
  }

  public void onProgramChange(int channel, int program) {
    if (channel == channel_) {
      listener_.onProgramChange(channel, program);
    }
  }

  public void onChannelAftertouch(int channel, int aftertouch) {
    if (channel == channel_) {
      listener_.onChannelAftertouch(channel, aftertouch);
    }
  }

  public void onPitchBend(int channel, int value) {
    if (channel == channel_) {
      listener_.onPitchBend(channel, value);
    }
  }

  public void onChannelPrefix(int channel) {
    if (channel == channel_) {
      listener_.onChannelPrefix(channel);
    }
  }

  // The channel whose events are forwarded.
  private int channel_;
}
