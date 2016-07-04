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

#include <iostream>
#include "midi_in_mac.h"

#define DEBUG_MIDI_BYTES

using std::cout;
using std::endl;

void MidiInMac::OnRead(const MIDIPacketList *pktlist) {
  const MIDIPacket *packet = &(pktlist->packet[0]);
  for (int i = 0; i < pktlist->numPackets; ++i) {
#if defined(DEBUG_MIDI_BYTES)
    cout << "MIDI DATA:";
    cout.fill('0');
    for (size_t i = 0; i < packet->length; i++) {
      cout << " ";
      cout.width(2);
      cout << std::hex << static_cast<int>(packet->data[i]);
      cout.width();
    }
    cout << endl;
#endif
    ring_buffer_->Write(packet->data, packet->length);
    packet = MIDIPacketNext(packet);
  }
}

extern "C" void ReadProc(const MIDIPacketList *pktlist, void *readProcRefCon,
    void *srcConnRefCon) {
  MidiInMac *self = (MidiInMac *)readProcRefCon;
  self->OnRead(pktlist);
}

bool MidiInMac::Init(CFStringRef name, RingBuffer *ring_buffer) {
  ring_buffer_ = ring_buffer;
  OSStatus s = MIDIClientCreate(CFSTR("synth"), NULL, NULL, &client_);
  if (s != noErr) return false;
  s = MIDIInputPortCreate(client_, CFSTR("synthin"), ReadProc, (void *)this,
      &port_);
  ItemCount n = MIDIGetNumberOfDevices();
  for (int i = 0; i < n; ++i) {
    MIDIDeviceRef device_ref = MIDIGetDevice(i);
    CFPropertyListRef midi_device_properties;
    MIDIObjectGetProperties(device_ref, &midi_device_properties, true);
    CFStringRef dev_name = NULL;
    s = MIDIObjectGetStringProperty(device_ref, kMIDIPropertyName, &dev_name);
#if defined(DEBUG_USB_NAMES)
    char buf[64];
    if (CFStringGetCString(dev_name, buf, sizeof(buf), kCFStringEncodingASCII)) {
      std::cout << "midi name =" << buf << std::endl;
    } else {
      std::cout << "error converting" << std::endl;
    }
#endif
    CFComparisonResult comparison = CFStringCompare(dev_name, name, 0);
    CFRelease(dev_name);
    if (comparison == kCFCompareEqualTo) {
      std::cout << "found!" << std::endl;
      MIDIEntityRef entity = MIDIDeviceGetEntity(device_ref, 0);
      MIDIEndpointRef endpoint_ = MIDIEntityGetSource(entity, 0);
      s = MIDIPortConnectSource(port_, endpoint_, NULL);
      return true;
    }
  }
  return false;
}

void MidiInMac::Done() {
  MIDIEndpointDispose(endpoint_);
}

