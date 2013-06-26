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

// Resonant filter implementation. This closely follows "Non-Linear
// Digital Implementation of the Moog Ladder Filter" by Antti
// Huovilainen, 2004.

// The full implementation requires both a tuning table and 2x
// oversampling, neither of which are present yet, but we'll get there. 

#include "synth.h"
#include "freqlut.h"
#include "exp2.h"
#include "resofilter.h"

double this_sample_rate;

void ResoFilter::init(double sample_rate) {
  this_sample_rate = sample_rate;
}

ResoFilter::ResoFilter() {
  for (int i = 0; i < 4; i++) {
    x[i] = 0;
#if defined(NONLINEARITY)
    w[i] = 0;
#endif
  }
}

int32_t compute_alpha(int32_t logf) {
  // TODO: better tuning
  return min(1 << 24, Freqlut::lookup(logf));
}

void ResoFilter::process(const int32_t **inbufs, const int32_t *control_in,
                         const int32_t *control_last, int32_t **outbufs) {
  int32_t alpha = compute_alpha(control_last[0]);
  int32_t alpha_in = compute_alpha(control_in[0]);
  int32_t delta_alpha = (alpha_in - alpha) >> lg_n;
  int32_t k = control_last[1];
  int32_t k_in = control_in[1];
  int32_t delta_k = (k_in - k) >> lg_n;
  if ((((int64_t)alpha_in * (int64_t)k_in) >> 24) > 1 << 24) {
    k_in = ((1 << 30) / alpha_in) << 18;
  }
  if ((((int64_t)alpha * (int64_t)k) >> 24) > 1 << 24) {
    k = ((1 << 30) / alpha) << 18;
  }
  const int32_t *ibuf = inbufs[0];
  int32_t *obuf = outbufs[0];
  int32_t x0 = x[0];
  int32_t x1 = x[1];
  int32_t x2 = x[2];
  int32_t x3 = x[3];
#if defined(NONLINEARITY)
  int32_t w0 = w[0];
  int32_t w1 = w[1];
  int32_t w2 = w[2];
  int32_t w3 = w[3];
  int32_t yy0 = yy;
#endif;
  for (int i = 0; i < n; i++) {
    alpha += delta_alpha;
    k += delta_k;
    int32_t signal = ibuf[i];
#if defined(NONLINEARITY) 
    int32_t fb = ((int64_t)k * (int64_t)(x3 + yy0)) >> 25;
    yy0 = x3;
    int32_t rx = signal - fb;
    int32_t trx = Tanh::lookup(rx);
    x0 = x0 + ((((int64_t)(trx - w0) * (int64_t)alpha)) >> 24);
    w0 = Tanh::lookup(x0);
    x1 = x1 + ((((int64_t)(w0 - w1) * (int64_t)alpha)) >> 24);
    w1 = Tanh::lookup(x1);
    x2 = x2 + ((((int64_t)(w1 - w2) * (int64_t)alpha)) >> 24);
    w2 = Tanh::lookup(x2);
    x3 = x3 + ((((int64_t)(w2 - w3) * (int64_t)alpha)) >> 24);
    w3 = Tanh::lookup(x3);
#else
    int32_t fb = ((int64_t)k * (int64_t)x3) >> 24;
    x0 = x0 + ((((int64_t)(signal - fb - x0) * (int64_t)alpha)) >> 24);
    x1 = x1 + ((((int64_t)(x0 - x1) * (int64_t)alpha)) >> 24);
    x2 = x2 + ((((int64_t)(x1 - x2) * (int64_t)alpha)) >> 24);
    x3 = x3 + ((((int64_t)(x2 - x3) * (int64_t)alpha)) >> 24);
#endif
    obuf[i] = x3;
  }
  x[0] = x0;
  x[1] = x1;
  x[2] = x2;
  x[3] = x3;
#if defined(NONLINEARITY)
  w[0] = w0;
  w[1] = w1;
  w[2] = w2;
  w[3] = w3;
  yy = yy0;
#endif
}
