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

#ifndef __SYNTH_SYNTH_MAIN_H
#define __SYNTH_SYNTH_MAIN_H

#include <AudioUnit/AudioUnit.h>

#include "synth.h"
#include "midi_in_mac.h"
#include "synth_unit.h"

class SynthMain {
 public:
  int SynthInit();
  int SynthDone();
  int Load(const char *filename);
 private:
  OSStatus setupplayback(SynthUnit *synth_unit);
  OSStatus startplayback();
  OSStatus stopplayback();
  AudioUnit audioUnit_;
  MidiInMac midi_in_mac_;
  RingBuffer ring_buffer_;
  SynthUnit *synth_unit_;
};

#endif  // __SYNTH_SYNTH_MAIN_H
