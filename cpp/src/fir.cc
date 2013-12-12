/*
 * Copyright 2013 Google Inc.
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

// Implementation of FIR filtering (convolution)

#include <stdio.h> // for debugging, remove
#include <stdlib.h>
#include <malloc.h>

#include "fir.h"

// Should probably ifdef this to make it more portable
void *malloc_aligned(size_t alignment, size_t nbytes) {
  return memalign(alignment, nbytes);
}

SimpleFirFilter::SimpleFirFilter(const float *kernel, size_t nk) : nk(nk) {
  k = (float *)malloc(nk * sizeof(k[0]));
  for (size_t i = 0; i < nk; i++) {
    k[i] = kernel[nk - i - 1];
  }
}

SimpleFirFilter::~SimpleFirFilter() {
  free(k);
}

void SimpleFirFilter::process(const float *in, float *out, size_t n) {
  for (size_t i = 0; i < n; i++) {
    float y = 0;
    for (size_t j = 0; j < nk; j++) {
      y += k[j] * in[i + j];
    }
    out[i] = y;
  }
}

NeonFirFilter::NeonFirFilter(const float *kernel, size_t nk) : nk(nk) {
  // TODO: handle odd size nk (must be multiple of 4)
  k = (float *)malloc_aligned(16, nk * sizeof(k[0]));
  for (size_t i = 0; i < nk; i += 4) {
    for (size_t j = 0; j < 4; j++) {
      k[i + j] = kernel[nk - i - 4 + j];
    }
  }
}

NeonFirFilter::~NeonFirFilter() {
  free(k);
}

#ifdef HAVE_NEON
extern "C"
void neon_fir_direct(const float *in, const float *k, float *out, size_t n, size_t nk);
#endif

void NeonFirFilter::process(const float *in, float *out, size_t n) {
  neon_fir_direct(in - 1, k, out, n, nk);
}
