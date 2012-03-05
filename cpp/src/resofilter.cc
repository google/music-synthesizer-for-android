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

#include "module.h"
#include "resofilter.h"

double this_sample_rate;

void ResoFilter::init(double sample_rate) {
  this_sample_rate = sample_rate;
}

ResoFilter::ResoFilter() {
  for (int i = 0; i < 4; i++) {
    x[i] = 0;
  }
}

int32_t compute_alpha(int32_t logf) {
  // TODO
  return 1 << 21;
}

void ResoFilter::process(const int32_t **inbufs, const int32_t *control_in,
                         const int32_t *control_last, int32_t **outbufs) {
  int32_t alpha = compute_alpha(control_last[0]);
  int32_t alpha_in = compute_alpha(control_in[0]);
  int32_t delta_alpha = (alpha_in - alpha) >> lg_n;
  int32_t k = control_last[1];
  int32_t k_in = control_in[1];
  int32_t delta_k = (k_in - k) >> lg_n;
  const int32_t *ibuf = inbufs[0];
  int32_t *obuf = outbufs[0];
  int x0 = x[0];
  int x1 = x[1];
  int x2 = x[2];
  int x3 = x[3];
  for (int i = 0; i < n; i++) {
    alpha += delta_alpha;
    k += delta_k;
    int32_t signal = ibuf[i]; 
    int32_t fb = ((int64_t)k * (int64_t)x3) >> 24;
    x0 = x0 + ((((int64_t)(signal - fb - x0) * (int64_t)alpha)) >> 24);
    x1 = x1 + ((((int64_t)(x0 - x1) * (int64_t)alpha)) >> 24);
    x2 = x2 + ((((int64_t)(x1 - x2) * (int64_t)alpha)) >> 24);
    x3 = x3 + ((((int64_t)(x2 - x3) * (int64_t)alpha)) >> 24);
    obuf[i] = x3;
  }
  x[0] = x0;
  x[1] = x1;
  x[2] = x2;
  x[3] = x3;
}
