/*
 * Copyright 2017 Google Inc.
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

// C-wrapped API entrypoints for use from Emscripten

#ifdef __EMSCRIPTEN__
#include <emscripten.h>
#else
#define EMSCRIPTEN_KEEPALIVE
#endif

#include "synth.h"
#include "synth_unit.h"

struct Synth {
    RingBuffer *buf;
    SynthUnit *synth_unit;
};

extern "C" {
    EMSCRIPTEN_KEEPALIVE
    Synth *synth_create(int sample_rate) {
        Synth *synth = new Synth();
        synth->buf = new RingBuffer();
        synth->synth_unit = new SynthUnit(synth->buf);
        synth->synth_unit->Init(sample_rate);
        return synth;
    }

    EMSCRIPTEN_KEEPALIVE
    void synth_get_samples(Synth *synth, int n_samples, int16_t *buffer) {
        synth->synth_unit->GetSamples(n_samples, buffer);
    }

    EMSCRIPTEN_KEEPALIVE
    void synth_send_midi(Synth *synth, const uint8_t *bytes, int size) {
        synth->buf->Write(bytes, size);
    }
}
