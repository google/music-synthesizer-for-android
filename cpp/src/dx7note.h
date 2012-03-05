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

#ifndef SYNTH_DX7NOTE_H_
#define SYNTH_DX7NOTE_H_

// This is the logic to put together a note from the MIDI description
// and run the low-level modules.

// It will continue to evolve a bit, as note-stealing logic, scaling,
// and real-time control of parameters live here.

#include "env.h"
#include "fm_core.h"

class Dx7Note {
 public:
  // Interesting question: should the setup be in the constructor, or should
  // there be an init method? The latter would make it easier to use a fixed
  // pool of note objects.
  Dx7Note(const char patch[128], int midinote, int velocity);

  // Note: this _adds_ to the buffer. Interesting question whether it's
  // worth it...
  void compute(int32_t *buf);

  void keyup();

  // TODO: parameter changes

  // TODO: some way of indicating end-of-note. Maybe should be a return
  // value from the compute method? (Having a count return from keyup
  // is also tempting, but if there's a dynamic parameter change after
  // keyup, that won't work.

 private:
  FmCore core_;
  Env env_[6];
  FmOpParams params_[6];
  int32_t fb_buf_[2];
  int32_t fb_shift_;

  int algorithm_;
};

#endif  // SYNTH_DX7NOTE_H_
