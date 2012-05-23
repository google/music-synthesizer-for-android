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

#include <math.h>

#include "module.h"
#include "sawtooth.h"
#include "freqlut.h"

// There's a fair amount of lookup table and so on that needs to be set before
// generating any signal. In Java, this would be done by a separate factory class.
// Here, we're just going to do it as globals.

#define HIGH_QUALITY

#define noLOW_FREQ_HACK

#define FANCY_GOERTZEL_SIN
#define noPRINT_ERROR

#define R (1 << 29)

#ifdef LOW_FREQ_HACK
#define LG_N_SAMPLES 9
#else
#define LG_N_SAMPLES 11
#endif
#define N_SAMPLES (1 << LG_N_SAMPLES)
#define N_PARTIALS_MAX (N_SAMPLES / 2)

#define LG_SLICES_PER_OCTAVE 2
#define SLICES_PER_OCTAVE (1 << LG_SLICES_PER_OCTAVE)
#define SLICE_SHIFT (24 - LG_SLICES_PER_OCTAVE)

#define LG_N_SLICES (LG_SLICES_PER_OCTAVE + 4)
#define N_SLICES (1 << LG_N_SLICES)

#define NEG2OVERPI -0.63661977236758138

int32_t sawtooth[N_SLICES][N_SAMPLES];

void Sawtooth::init(double sample_rate) {
  int32_t lut[N_SAMPLES / 2];

  for (int i = 0; i < N_SAMPLES / 2; i++) {
    lut[i] = 0;
  }

  double slice_inc = pow(2.0, 1.0 / SLICES_PER_OCTAVE);
  double f_0 = pow(slice_inc, N_SLICES - 1);
  int n_partials_last = 0;
  for (int j = N_SLICES - 1; j >= 0; j--) {
    int n_partials = floor(0.5 * sample_rate / f_0);
    n_partials = n_partials < N_PARTIALS_MAX ? n_partials : N_PARTIALS_MAX;
    for (int k = n_partials_last + 1; k <= n_partials; k++) {
      double scale = NEG2OVERPI / k;
      scale = (N_PARTIALS_MAX - k) > (N_PARTIALS_MAX >> 2) ? scale :
        scale * (N_PARTIALS_MAX - k) / (N_PARTIALS_MAX >> 2);
      double dphase = k * 2 * M_PI / N_SAMPLES;
#ifdef PRINT_ERROR
      int32_t maxerr = 0;
#endif
#ifdef FANCY_GOERTZEL_SIN
      double ds_d = (1 << 30) * scale * sin(dphase);
      double cm2_d = (1 << 29) * (2 * (cos(dphase) - 1));
      int dshift = 0;
      for (dshift = 0; dshift < 16; dshift++) {
        if (ds_d < -(1 << (30 - dshift))) break;
        if (cm2_d < -(1 << (30 - dshift))) break;
      }
      int32_t ds = (int32_t)floor((1 << dshift) * ds_d + 0.5);
      int32_t cm2 = (int32_t)floor((1 << dshift) * cm2_d + 0.5);
      // cout << cm2_d << " " << cm2 << " " << dphase << " " << ds << " " << dshift << endl;
      int32_t s = 0;
      int32_t round = (1 << dshift) >> 1;
      for (int i = 0; i < N_SAMPLES / 2; i++) {
        lut[i] += s;
#ifdef PRINT_ERROR
        int32_t good = (int32_t)floor(scale * sin(dphase * i) * (1 << 30) + 0.5);
        int err = s - good;
        int abs_err = err > 0 ? err : -err;
        maxerr = abs_err > maxerr ? abs_err : maxerr;
#endif
        ds += ((int64_t)cm2 * (int64_t)s + R) >> 29;
        s += (ds + round) >> dshift;
      }
#else
      int32_t c = (int32_t)floor(cos(dphase) * (1 << 30) + 0.5);
      int32_t s = (int32_t)floor(sin(dphase) * (1 << 30) + 0.5);
      int32_t u = (int32_t)floor(scale * (1 << 30));
      int32_t v = 0;
      for (int i = 0; i < N_SAMPLES / 2; i++) {
        lut[i] += v;
#ifdef PRINT_ERROR
        int32_t good = (int32_t)floor(scale * sin(dphase * i) * (1 << 30) + 0.5);
        int err = v - good;
        int abs_err = err > 0 ? err : -err;
        maxerr = abs_err > maxerr ? abs_err : maxerr;
#endif
        int32_t t = ((int64_t)u * (int64_t)s + (int64_t)v * (int64_t)c + R) >> 30;
        u = ((int64_t)u * (int64_t)c - (int64_t)v * (int64_t)s + R) >> 30;
        v = t;
      }
#endif
#ifdef PRINT_ERROR
      cout << maxerr << endl;
#endif
    }
    sawtooth[j][0] = 0;
    sawtooth[j][N_SAMPLES / 2] = 0;
    for (int i = 1; i < N_SAMPLES / 2; i++) {
      int32_t value = (lut[i] + 32) >> 6;
      sawtooth[j][i] = value;
      sawtooth[j][N_SAMPLES - i] = -value;
    }
    n_partials_last = n_partials;
    f_0 *= 1.0 / slice_inc;
  }
}

Sawtooth::Sawtooth() {
  phase = 0;
}

int32_t Sawtooth::lookup(int32_t phase, int32_t log_f) {

  log_f = log_f < 0 ? 0 : log_f;
  int slice = (log_f + (1 << SLICE_SHIFT) - 1) >> SLICE_SHIFT;
  int phase_int = (phase >> (24 - LG_N_SAMPLES)) & (N_SAMPLES - 1);
  int lowbits = phase & ((1 << (24 - LG_N_SAMPLES)) - 1);
  int y0 = sawtooth[slice][phase_int];
  int y1 = sawtooth[slice][(phase_int + 1) & (N_SAMPLES - 1)];

  int y4 = y0 + ((((int64_t)(y1 - y0) * (int64_t)lowbits)) >> (24 - LG_N_SAMPLES));

  // TODO: lift this out of loop
  // TODO: optimal threshold probably depends on sample rate
#ifdef LOW_FREQ_HACK
  if (log_f < (8 << 24))
    y4 = phase * 2 - (1 << 24);
#endif

#ifdef HIGH_QUALITY
  int y2 = sawtooth[slice + 1][phase_int];
  int y3 = sawtooth[slice + 1][(phase_int + 1) & (N_SAMPLES - 1)];
  int y5 = y2 + ((((int64_t)(y3 - y2) * (int64_t)lowbits)) >> (24 - LG_N_SAMPLES));
#ifdef LOW_FREQ_HACK
  if (log_f < (8 << 24) - (1 << SLICE_SHIFT))
    y5 = phase * 2 - (1 << 24);
#endif
  int slice_lowbits = log_f & ((1 << SLICE_SHIFT) - 1);
  int y = y4 + ((((int64_t)(y5 - y4) * (int64_t)slice_lowbits)) >> SLICE_SHIFT);
  return y;
#else
  return y4;
#endif
}

void Sawtooth::process(const int32_t **inbufs, const int32_t *control_in,
                       const int32_t *control_last, int32_t **outbufs) {
  int32_t logf = control_last[0];
  int32_t logf_in = control_in[0];
  int32_t *obuf = outbufs[0];
  int32_t delta_logf = (logf_in - logf) >> lg_n;
  int f = Freqlut::lookup(logf);
  int f_in = Freqlut::lookup(logf_in);
  int32_t delta_f = (f_in - f) >> lg_n;
  int32_t p = phase;
  for (int i = 0; i < n; i++) {
    f += delta_f;
    logf += delta_logf;
    obuf[i] = lookup(p, logf);
    p += f;
    p &= (1 << 24) - 1;
  }
  phase = p;
}
