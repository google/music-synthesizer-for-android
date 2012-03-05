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

#include "dx7note.h"
#include "ringbuffer.h"

struct ActiveNote {
  int midi_note;
  Dx7Note *dx7_note;
};

class SynthUnit {
 public:
  explicit SynthUnit(RingBuffer *ring_buffer);

  void GetSamples(int n_samples, int16_t *buffer);
 private:
  void TransferInput();

  void ConsumeInput(int n_input_bytes);

  int ProcessMidiMessage(const uint8_t *buf, int buf_size);

  RingBuffer *ring_buffer_;
  static const int max_active_notes = 16;
  ActiveNote active_note_[max_active_notes];
  int current_note_;
  uint8_t input_buffer_[8192];
  size_t input_buffer_index_;

  uint8_t patch_data_[4096];
  int current_patch_;
};
