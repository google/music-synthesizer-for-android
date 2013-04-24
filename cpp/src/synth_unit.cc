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

#ifdef VERBOSE
#include <iostream>
#endif

#include <string.h>

#include "synth.h"
#include "synth_unit.h"
#include "aligned_buf.h"

char epiano[] = {
  95, 29, 20, 50, 99, 95, 0, 0, 41, 0, 19, 0, 115, 24, 79, 2, 0,
  95, 20, 20, 50, 99, 95, 0, 0, 0, 0, 0, 0, 3, 0, 99, 2, 0,
  95, 29, 20, 50, 99, 95, 0, 0, 0, 0, 0, 0, 59, 24, 89, 2, 0,
  95, 20, 20, 50, 99, 95, 0, 0, 0, 0, 0, 0, 59, 8, 99, 2, 0,
  95, 50, 35, 78, 99, 75, 0, 0, 0, 0, 0, 0, 59, 28, 58, 28, 0,
  96, 25, 25, 67, 99, 75, 0, 0, 0, 0, 0, 0, 83, 8, 99, 2, 0,
  
  94, 67, 95, 60, 50, 50, 50, 50, 4, 6, 34, 33, 0, 0, 56, 24,
  69, 46, 80, 73, 65, 78, 79, 32, 49, 32
};

SynthUnit::SynthUnit(RingBuffer *ring_buffer) {
  ring_buffer_ = ring_buffer;
  for (int note = 0; note < max_active_notes; ++note) {
    active_note_[note].dx7_note = NULL;
    active_note_[note].keydown = false;
    active_note_[note].sustained = false;
  }
  input_buffer_index_ = 0;
  memcpy(patch_data_, epiano, sizeof(epiano));
  current_patch_ = 0;
  current_note_ = 0;
  filter_control_[0] = 258847126;
  filter_control_[1] = 0;
  sustain_ = false;
}

// Transfer as many bytes as possible from ring buffer to input buffer.
// Note that this implementation has a fair amount of copying - we'd probably
// do it a bit differently if it were bulk data, but in this case we're
// optimizing for simplicity of implementation.
void SynthUnit::TransferInput() {
  size_t bytes_available = ring_buffer_->BytesAvailable();
  int bytes_to_read = min(bytes_available,
      sizeof(input_buffer_) - input_buffer_index_);
  if (bytes_to_read > 0) {
    ring_buffer_->Read(bytes_to_read, input_buffer_ + input_buffer_index_);
    input_buffer_index_ += bytes_to_read;
  }
}

void SynthUnit::ConsumeInput(int n_input_bytes) {
  if (n_input_bytes < input_buffer_index_) {
    memmove(input_buffer_, input_buffer_ + n_input_bytes,
        input_buffer_index_ - n_input_bytes);
  }
  input_buffer_index_ -= n_input_bytes;
}

int SynthUnit::AllocateNote() {
  int note = current_note_;
  for (int i = 0; i < max_active_notes; i++) {
    if (!active_note_[note].keydown) {
      current_note_ = (note + 1) % max_active_notes;
      return note;
    }
    note = (note + 1) % max_active_notes;
  }
  return -1;
}

int SynthUnit::ProcessMidiMessage(const uint8_t *buf, int buf_size) {
  uint8_t cmd = buf[0];
  uint8_t cmd_type = cmd & 0xf0;
  if (cmd_type == 0x80 || (cmd_type == 0x90 && buf[2] == 0)) {
    if (buf_size >= 3) {
      // note off
      for (int note = 0; note < max_active_notes; ++note) {
        if (active_note_[note].midi_note == buf[1] && 
            active_note_[note].keydown) {
          if (sustain_) {
            active_note_[note].sustained = true;
          } else {
            active_note_[note].dx7_note->keyup();
          }
          active_note_[note].keydown = false;
        }
      }
      return 3;
    }
    return 0;
  } else if (cmd_type == 0x90) {
    if (buf_size >= 3) {
      // note on
      int note_ix = AllocateNote();
      if (note_ix >= 0) {
        delete active_note_[note_ix].dx7_note;
        active_note_[note_ix].midi_note = buf[1];
        active_note_[note_ix].keydown = true;
        active_note_[note_ix].sustained = sustain_;
        const uint8_t *patch = patch_data_ + 128 * current_patch_;
        active_note_[note_ix].dx7_note =
          new Dx7Note((const char *)patch, buf[1], buf[2]);
      }
      return 3;
    }
    return 0;
  } else if (cmd_type == 0xb0) {
    if (buf_size >= 3) {
      int controller = buf[1];
      int value = buf[2];
      if (controller == 1) {
        filter_control_[0] = 129423563 + value * 1019083;
      } else if (controller == 2) {
        filter_control_[1] = value * 528416;
      } else if (controller == 64) {
        sustain_ = value != 0;
        if (!sustain_) {
          for (int note = 0; note < max_active_notes; note++) {
            if (active_note_[note].sustained && !active_note_[note].keydown) {
              active_note_[note].dx7_note->keyup();
              active_note_[note].sustained = false;
            }
          }
        }
      }
      return 3;
    } return 0;
  } else if (cmd_type == 0xc0) {
    if (buf_size >= 2) {
      // program change
      int program_number = buf[1];
      current_patch_ = min(program_number, 31);
      char name[11];
      memcpy(name, patch_data_ + 128 * current_patch_ + 118, 10);
      name[10] = 0;
#ifdef VERBOSE
      std::cout << "Loaded patch " << current_patch_ << ": " << name << "\r";
      std::cout.flush();
#endif
      return 2;
    }
    return 0;
  } else if (cmd == 0xf0) {
    // sysex
    if (buf_size >= 6 && buf[1] == 0x43 && buf[2] == 0x00 && buf[3] == 0x09 &&
        buf[4] == 0x20 && buf[5] == 0x00) {
      if (buf_size >= 4104) {
        // TODO: check checksum?
        memcpy(patch_data_, buf + 6, 4096);
        return 4104;
      }
      return 0;
    }
  }

  // TODO: more robust handling
#ifdef VERBOSE
  std::cout << "Unknown message " << std::hex << (int)cmd <<
    ", skipping " << std::dec << buf_size << " bytes" << std::endl;
#endif
  return buf_size;
}

void SynthUnit::GetSamples(int n_samples, int16_t *buffer) {
  TransferInput();
  size_t input_offset;
  for (input_offset = 0; input_offset < input_buffer_index_; ) {
    int bytes_available = input_buffer_index_ - input_offset;
    int bytes_consumed = ProcessMidiMessage(input_buffer_ + input_offset,
        bytes_available);
    if (bytes_consumed == 0) {
      break;
    }
    input_offset += bytes_consumed;
  }
  ConsumeInput(input_offset);

  for (int i = 0; i < n_samples; i += N) {
    AlignedBuf<int32_t, N> audiobuf;
    int32_t audiobuf2[N];
    for (int j = 0; j < N; ++j) {
      audiobuf.get()[j] = 0;
    }
    for (int note = 0; note < max_active_notes; ++note) {
      if (active_note_[note].dx7_note != NULL) {
        active_note_[note].dx7_note->compute(audiobuf.get());
      }
    }
    const int32_t *bufs[] = { audiobuf.get() };
    int32_t *bufs2[] = { audiobuf2 };
    filter_.process(bufs, filter_control_, filter_control_, bufs2);
    for (int j = 0; j < N; ++j) {
      int32_t val = audiobuf2[j] >> 4;
      int clip_val = val < -(1 << 24) ? 0x8000 : val >= (1 << 24) ? 0x7fff :
        val >> 9;
      // TODO: maybe some dithering?
      buffer[i + j] = clip_val;
    }
  }
}
