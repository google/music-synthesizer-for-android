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

#include "aligned_buf.h"
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

HalfRateFirFilter::HalfRateFirFilter(const float *kernel, size_t nk, size_t n) : nk(nk) {
  float k0[kMaxNk / 2];
  float k1[kMaxNk / 2];
  size_t n2 = n >> 1;
  size_t nk2 = nk >> 1;
  // probably better to do fewer allocations and just set up pointers...
  y0 = (float *)malloc_aligned(16, n2 * sizeof(y0[0]));
  y1 = (float *)malloc_aligned(16, n2 * sizeof(y1[0]));
  y2 = (float *)malloc_aligned(16, n2 * sizeof(y2[0]));
  i0 = (float *)malloc_aligned(16, (n2 + nk2) * sizeof(i0[0]));
  i1 = (float *)malloc_aligned(16, (n2 + nk2) * sizeof(i1[0]));
  i2 = (float *)malloc_aligned(16, (n2 + nk2) * sizeof(i2[0]));
  k2 = (float *)malloc_aligned(16, nk2 * sizeof(k2[0]));
  for (size_t i = 0; i < nk2; i++) {
    float b0 = kernel[i * 2];
    float b2 = kernel[i * 2 + 1];
    k0[i] = b0;
    k1[i] = b0 + b2;
    k2[i] = b2;
  }
  f0 = new SimpleFirFilter(k0, nk2);
  f1 = new SimpleFirFilter(k1, nk2);
  f2 = new SimpleFirFilter(k2, nk2);
}

HalfRateFirFilter::~HalfRateFirFilter() {
  free(k2);
  delete i0;
  delete i1;
  delete i2;
  delete y0;
  delete y1;
  delete y2;
  delete f0;
  delete f1;
  delete f2;
}

extern "C"
void neon_halfrate_split(const float *in, float *buf0, float *buf1, float *buf2, size_t n);

extern "C"
void neon_halfrate_combine(const float *out, float *buf0, float *buf1, float *buf2, size_t n);

void HalfRateFirFilter::process(const float *in, float *out, size_t n) {
  size_t n2 = n >> 1;
  size_t nk2 = nk >> 1;
  size_t n2in = n2 + nk2 - 1;
#ifdef HAVE_NEON
  neon_halfrate_split(in - 1, i0, i1, i2, n2in + 1);
#else
  i2[0] = in[0];
  for (size_t i = 0; i < n2in; i++) {
    float a0 = in[i * 2 + 1];
    float a2 = in[i * 2 + 2];
    i0[1 + i] = a0;
    i1[1 + i] = a0 + a2;
    i2[1 + i] = a2;
  }
#endif
  f0->process(i0 + 1, y0, n2);
  f1->process(i1 + 1, y1, n2);
  f2->process(i2 + 1, y2, n2);
#ifdef HAVE_NEON
  neon_halfrate_combine(out, y0, y1, y2, n2);
#else
  float z2m2 = 0;
  for (size_t i = 0; i < nk2; i++) {
    z2m2 += k2[nk2 - 1 - i] * i2[i];
  }
  for (size_t i = 0; i < n2; i++) {
    float m0 = y0[i];
    float m1 = y1[i];
    float m2 = y2[i];
    out[i * 2] = m0 + z2m2;
    out[i * 2 + 1] = m1 - m0 - m2;
    //out[i*2] = i1.get()[i];
    z2m2 = m2;
  }
#endif
}

#ifdef HAVE_NEON
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

extern "C"
void neon_fir_direct(const float *in, const float *k, float *out, size_t n, size_t nk);

void NeonFirFilter::process(const float *in, float *out, size_t n) {
  neon_fir_direct(in - 1, k, out, n, nk);
}

Neon16FirFilter::Neon16FirFilter(const float *kernel, size_t nk, bool mirror)
  : nk(nk), mirror(mirror) {
  // TODO: handle odd size nk (must be multiple of 4)
  k = (int16_t *)malloc_aligned(16, nk * sizeof(k[0]));
  for (size_t i = 0; i < nk; i++) {
    k[i] = 32768 * kernel[nk - i - 1];
  }
}

Neon16FirFilter::~Neon16FirFilter() {
  free(k);
}

extern "C"
void neon_fir_fixed16(const float *in, const int16_t *k, float *out, size_t n, size_t nk);

extern "C"
void neon_fir_fixed16m(const float *in, const int16_t *k, float *out, size_t n, size_t nk);

void Neon16FirFilter::process(const float *in, float *out, size_t n) {
  if (mirror)
    neon_fir_fixed16m(in - 1, k, out, n, nk);
  else
    neon_fir_fixed16(in - 1, k, out, n, nk);
}

#endif
