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

// Interface for receiving MIDI events

#include <CoreMIDI/CoreMIDI.h>

#include "ringbuffer.h"

// At some point, we may want to have a generic MidiIn interface that gets
// implemented differently on different platforms, but for now we keep it
// simple (threading and lifetime might be different on other platforms,
// so it's not obvious what the interface might look like).
class MidiInMac {
 public:
  //MidiInMac();

  // Return true on success. While running (ie until Done() is called,
  // MIDI bytes from the device are written to the ring buffer.
  bool Init(CFStringRef name, RingBuffer *ring_buffer);

  void Done();

  // Effectively private - only called from ReadProc
  void OnRead(const MIDIPacketList *pktlist);
 private:

  RingBuffer *ring_buffer_;
  MIDIClientRef client_;
  MIDIPortRef port_;
  MIDIEndpointRef endpoint_;
};

