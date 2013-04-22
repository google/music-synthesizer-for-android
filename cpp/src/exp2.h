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

class Exp2 {
 public:
  Exp2();

  static void init();

  // Q24 in, Q24 out
  static int32_t lookup(int32_t x);
};

#define EXP2_LG_N_SAMPLES 10
#define EXP2_N_SAMPLES (1 << EXP2_LG_N_SAMPLES)

#define EXP2_INLINE

extern int32_t exp2tab[EXP2_N_SAMPLES << 1];

#ifdef EXP2_INLINE
inline
int32_t Exp2::lookup(int32_t x) {
  const int SHIFT = 24 - EXP2_LG_N_SAMPLES;
  int lowbits = x & ((1 << SHIFT) - 1);
  int x_int = (x >> (SHIFT - 1)) & ((EXP2_N_SAMPLES - 1) << 1);
  int dy = exp2tab[x_int];
  int y0 = exp2tab[x_int + 1];

  int y = y0 + (((int64_t)dy * (int64_t)lowbits) >> SHIFT);
  return y >> (6 - (x >> 24));
}
#endif
