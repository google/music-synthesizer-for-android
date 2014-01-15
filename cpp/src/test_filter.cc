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

// Little test app for measuring FIR speed

#include <stdlib.h>
#include <stdio.h>
#include <time.h>
#include <sys/time.h>
#include <math.h>

#include "aligned_buf.h"
#include "fir.h"

// clock_gettime would be a little better, but whatever
double now() {
  struct timeval tp;
  gettimeofday(&tp, NULL);
  return tp.tv_sec + 1e-6 * tp.tv_usec;
}

void condition_governor() {
  // sleep for a bit to avoid thermal throttling
  static uint32_t v = 0;
  struct timespec ts;
  ts.tv_sec = 0;
  ts.tv_nsec = 900000000 + (v & 1); // 900ms
  //nanosleep(&ts, NULL);

  // consume cpu a bit to try to coax max cpufreq
  uint32_t x = v;
  for (int i = 0; i < 10000000; i++) {
    x += 42;
    x += (x << 10);
    x ^= (x >> 6);
  }
  // storing it in a static guarantees not optimizing out
  v = x;
}

float *mkrandom(size_t size) {
  float *result = (float *)malloc_aligned(16, size * sizeof(result[0]));
  for (int i = 0; i < size; i++) {
    result[i] = random() * (2.0 / RAND_MAX) - 1;
  }
  return result;
}

double test_accuracy(FirFilter<float, float> *f1, FirFilter<float, float> *f2, const float *inp, int nblock) {
  float *out1 = (float *)malloc_aligned(16, nblock * sizeof(out1[0]));
  float *out2 = (float *)malloc_aligned(16, nblock * sizeof(out2[0]));
  f1->process(inp + 1, out1, nblock);
  f2->process(inp + 1, out2, nblock);
  double err = 0;
  for (int i = 0; i < nblock; i++) {
    printf("#%d: %f %f\n", i, out1[i], out2[i]);
    err += fabs(out1[i] - out2[i]);
  }
  free(out1);
  free(out2);
  return err;
}

void benchfir(int size, int experiment) {
  condition_governor();

  const int nblock = 64;
  float *kernel = mkrandom(size);
  float *inp = mkrandom(size + nblock);
  float *out = (float *)malloc_aligned(16, nblock * sizeof(out[0]));
  FirFilter<float, float> *f;

  switch(experiment) {
    case 0:
      f = new SimpleFirFilter(kernel, size);
      break;
#ifdef HAVE_NEON
    // this will crash on non-NEON devices, but we're only interested
    // in testing NEON for now
    case 1:
      f = new NeonFirFilter(kernel, size);
      break;
    case 2:
    case 3:
      f = new Neon16FirFilter(kernel, size, experiment == 3);
      break;
#endif
    case 4:
      f = new HalfRateFirFilter(kernel, size, nblock);
      break;
  }


  double start = now();
  for (int j = 0; j < 15625; j++) {
    f->process(inp + 1, out, nblock);
  }
  double elapsed = now() - start;
  printf("%i %f\n", size, 1e3 * elapsed);

  FirFilter<float, float> *fbase = new SimpleFirFilter(kernel, size);
  double accuracy = test_accuracy(fbase, f, inp, nblock);
  printf("#accuracy = %g\n", accuracy);

  delete f;
  delete fbase;
  free(kernel);
  free(inp);
  free(out);
}

void runfirbench() {
  printf("set style data linespoints\n"
    "set xlabel 'FIR kernel size'\n"
    "set ylabel 'ns per sample'\n"
    "plot '-' title 'scalar', '-' title '4x4 block', '-' title 'fixed16', '-' title 'fixed16 mirror', '-' title 'half rate'\n");
  for (int experiment = 0; experiment < 5; experiment++) {
    for (int i = 16; i <= 256; i += 16) {
      benchfir(i, experiment);
    }
    printf("e\n");
  }
}

void scalarbiquad(const float *inp, float *out, size_t n,
  float b0, float b1, float b2, float a1, float a2) {
  float x1 = 0, x2 = 0, y1 = 0, y2 = 0;
  for (size_t i = 0; i < n; i++) {
    float x = inp[i];
    float y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;
    out[i] = y;
    x2 = x1;
    x1 = x;
    y2 = y1;
    y1 = y;
  }
}

void benchscalarbiquad() {
  condition_governor();
  const int nbuf = 1 << 10;
  float *inp = mkrandom(nbuf);
  float *out = (float *)malloc_aligned(16, nbuf * sizeof(out[0]));

  double start = now();
  const int niter = 10000;
  for (int i = 0; i < niter; i++) {
    scalarbiquad(inp, out, nbuf, 1.0207, -1.7719, .9376, -1.7719, 0.9583);
  }
  double elapsed = now() - start;
  double ns_per_iir = 1e9 * elapsed / nbuf / niter;
  printf("scalar: %f ns/iir\n", ns_per_iir);

  free(inp);
  free(out);
}

extern "C"
void neon_iir_2chan(const float *in1, const float *in2, float *out1, float *out2,
  size_t n, const float *matrices, float *state);

// see "lab/biquadin two.ipynb" for why
void initbiquadmatrix(float *matrix, double b0, double b1, double b2, double a1, double a2) {
  double c1 = b1 - a1 * b0;
  double c2 = b2 - a2 * b0;
  matrix[0] = b0;
  matrix[1] = c1;
  matrix[2] = -a1 * c1 + c2;
  matrix[3] = -a2 * c1;
  matrix[4] = 0;
  matrix[5] = b0;
  matrix[6] = c1;
  matrix[7] = c2;
  matrix[8] = 1;
  matrix[9] = -a1;
  matrix[10] = -a2 + a1 * a1;
  matrix[11] = a1 * a2;
  matrix[12] = 0;
  matrix[13] = 1;
  matrix[14] = -a1;
  matrix[15] = -a2;
}

#ifdef HAVE_NEON
void benchbiquadneon() {
  const int nbuf = 1 << 10;
  float *inp1 = mkrandom(nbuf);
  float *inp2 = mkrandom(nbuf);
  float *out1 = (float *)malloc_aligned(16, nbuf * sizeof(out1[0]));
  float *out2 = (float *)malloc_aligned(16, nbuf * sizeof(out2[0]));
  AlignedBuf<float, 32> matrices;
  AlignedBuf<float, 4> state;

  for (size_t i = 0; i < 4; i++) {
    state.get()[i] = 0;
  }

  double start = now();
  const int niter = 100000;
  for (int i = 0; i < niter; i++) {
    neon_iir_2chan(inp1, inp2, out1, out2, nbuf, matrices.get(), state.get());
  }

  double elapsed = now() - start;
  double ns_per_iir = 1e9 * 0.5 * elapsed / nbuf / niter;
  printf("neon: %f ns/iir\n", ns_per_iir);
  free(inp1);
  free(inp2);
  free(out1);
  free(out2);
}

void testbiquadaccuracy() {
  const int nbuf = 1 << 10;
  float *inp1 = mkrandom(nbuf);
  float *inp2 = mkrandom(nbuf);
  float *out1 = (float *)malloc_aligned(16, nbuf * sizeof(out1[0]));
  float *out2 = (float *)malloc_aligned(16, nbuf * sizeof(out2[0]));
  float *out1a = (float *)malloc_aligned(16, nbuf * sizeof(out1[0]));
  float *out2a = (float *)malloc_aligned(16, nbuf * sizeof(out2[0]));
  AlignedBuf<float, 32> matrices;
  AlignedBuf<float, 4> state;
  double b0 = 1.0207, b1 = -1.7719, b2 = .9376, a1 = -1.7719, a2 = 0.9583;

  for (size_t i = 0; i < 4; i++) {
    state.get()[i] = 0;
  }

  initbiquadmatrix(matrices.get(), b0, b1, b2, a1, a2);
  initbiquadmatrix(matrices.get() + 16, b0, b1, b2, a1, a2);

  neon_iir_2chan(inp1, inp2, out1, out2, nbuf, matrices.get(), state.get());

  scalarbiquad(inp1, out1a, nbuf, b0, b1, b2, a1, a2);

  float maxerr = 0;
  for (int i = 0; i < nbuf; i++) {
    float err = fabs(out1[i] - out1a[i]);
    if (err > maxerr) {
      maxerr = err;
    }
  }
  printf("neon maxerr = %g\n", maxerr);
  free(inp1);
  free(inp2);
  free(out1);
  free(out2);
  free(out1a);
  free(out2a);
}

#endif

void runbiquad() {
  benchscalarbiquad();
#ifdef HAVE_NEON
  benchbiquadneon();
  testbiquadaccuracy();
#endif
}

int main(int argc, char **argv) {
  if (argc == 2) {
    if (!strcmp("fir", argv[1])) {
      runfirbench();
      return 0;
    } else if (!strcmp("biquad", argv[1])) {
      runbiquad();
      return 0;
    }
  }
  printf("usage:\n"
    "  test_filter fir\n"
    "  test_filter biquad\n");
  return 1;
}
