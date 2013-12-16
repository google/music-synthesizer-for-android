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

int main(int argc, char **argv) {
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
  return 0;
}
