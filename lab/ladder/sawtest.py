# Copyright 2013 Google Inc.
# 
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# 
#      http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Script for generating sound samples for comparing ladder filters

import argparse
import struct
import numpy as np
from math import *

samplerate = 44100 * 1

# generate a saw matching the parameters of "fun with sigmoids"
def sawperiod():
	result = []
	f0 = 63. * 2 * pi / samplerate
	for i in range(samplerate / 63):
		y = 0
		x0 = i * f0
		for partial in range(1, 351):
			gain = 1.0 / partial
			if partial > 300:
				gain *= (351 - partial) * (1.0 / (351 - 300))
			y += gain * sin(partial * x0)
		result.append(y)
	return result

def saw(n):
	return sawperiod() * (n / (samplerate / 63))

def sinsweep(n):
	fmax = 22000 * pi * 2 / samplerate
	scale = fmax * .5 / n
	return [sin(i * i * scale) for i in range(n)]

def sweep(n):
	n2 = n / 2
	lamin = log(20 * 2 * pi / samplerate)
	lamax = log(14000 * 2 * pi / samplerate)
	result = []
	slope = (lamax - lamin) / n2
	for i in range(n2):
		a = exp(lamin + slope * i)
		result.append(a)
	return result + result[-1::-1]

# Based on code by mystran (Teemu Voipio)
# See http://www.kvraudio.com/forum/viewtopic.php?t=349859

def tanhXdx(x):
	a = x * x
	return ((a + 105.0)* a + 945.0) / ((15.0 * a + 420.0) * a + 945.0)

def tpt_nl(xs, aas, k):
	z1 = 0
	s = [0] * 4
	r = k
	result = []
	for i in range(len(xs)):
		xin = xs[i]
		a = aas[i]
		f = tan(a * .5)
		ih = .5 * (xin + z1)
		z1 = xin

		t0 = tanhXdx(ih - r * s[3])
		t1 = tanhXdx(s[0])
		t2 = tanhXdx(s[1])
		t3 = tanhXdx(s[2])
		t4 = tanhXdx(s[3])
		g0 = 1 / (1 + f * t1)
		g1 = 1 / (1 + f * t2)
		g2 = 1 / (1 + f * t3)
		g3 = 1 / (1 + f * t4)
		f3 = f * t3 * g3
		f2 = f * t2 * g2 * f3
		f1 = f * t1 * g1 * f2
		f0 = f * t0 * g0 * f1
		y3 = (g3*s[3] + f3*g2*s[2] + f2*g1*s[1] + f1*g0*s[0] + f0*xin)/(1 + r * f0)
		xx = t0 * (xin - r * y3)
		y0 = t1 * g0 * (s[0] + f * xx)
		y1 = t2 * g1 * (s[1] + f * y0)
		y2 = t3 * g2 * (s[2] + f * y1)
		s[0] += 2 * f * (xx - y0)
		s[1] += 2 * f * (y0 - y1)
		s[2] += 2 * f * (y1 - y2)
		s[3] += 2 * f * (y2 - t4 * y3)
		result.append(y3)
	return result

def antti_nl(xs, aas, k, tanhfunc = tanh):
	y0 = 0
	y1 = 0
	y2 = 0
	y3 = 0
	ty0 = 0
	ty1 = 0
	ty2 = 0
	ty3 = 0
	yy = 0
	result = []
	for i in range(len(xs)):
		xin = xs[i]
		a = aas[i]
		tx = tanhfunc(xin - k * (y3 + yy) * .5)
		y0 += a * (tx - ty0)
		yy = y3
		ty0 = tanhfunc(y0)
		y1 += a * (ty0 - ty1)
		ty1 = tanhfunc(y1)
		y2 += a * (ty1 - ty2)
		ty2 = tanhfunc(y2)
		y3 += a * (ty2 - ty3)
		ty3 = tanhfunc(y3)
		x = 0
		result.append(y3)
	return result

def expm_series(A, n = 16):
	B = np.identity(len(A))
	C = np.identity(len(A))
	for i in range(1, n):
		C = A * C / i
		B = B + C
	return B

def expm_hyb(A, n1 = 8, n2 = 8):
	A = expm_series(A / (1 << n2), n1)
	for i in range(n2):
		A = A * A
	return A	

def mkjacobian(a, k):
	return np.matrix([[0, 0, 0, 0, 0],
		[a, -a, 0, 0, -k * a],
		[0, a, -a, 0, 0],
		[0, 0, a, -a, 0],
		[0, 0, 0, a, -a]])

def expm_nl(xs, aas, k, tanhfunc = tanh, every = 64):
	kk = k
	y = np.zeros([4, 1])
	ty = y
	result = []
	for i in range(len(xs)):
		if i % every == 0:
			a = aas[i]
			A = expm_hyb(mkjacobian(a, k))

			B = A[1:, 0]
			A = A[1:, 1:]
			AM = A - np.identity(4)

			for j in range(4):
				AM[j, 3] += B[j, 0] * kk

		x = xs[i]
		tx = tanhfunc(x - kk * y[3, 0])
		y += B * tx + AM * ty
		ty = np.matrix([tanhfunc(x[0]) for x in y]).T

		result.append(y[3, 0])
	return result

def invsqrt(x):
	return x / sqrt(1 + x ** 2)

def clip(x):
	return min(1, max(-1, x))

fir = map(float, '''-0.00000152158394097619
-0.00000932875737718674
-0.00001008290833020705
 0.00000728628960094510
 0.00002272556429291851
-0.00000017886444625648
-0.00004038828866646377
-0.00002127235603321839
 0.00005623314602326363
 0.00006233931357689778
-0.00005884569707596701
-0.00012410666372671377
 0.00003231781361429016
 0.00019973378481126099
 0.00004097428652472217
-0.00027125558280978066
-0.00017513777576291543
 0.00030833229435342778
 0.00037363561338823163
-0.00027028256701905416
-0.00062153272838533420
 0.00011242294792251808
 0.00087909017443692499
 0.00020309303892565388
-0.00107928469499717137
-0.00069305675670084180
 0.00113148992142813524
 0.00133794323705832747
-0.00093286682198106608
-0.00206892788224323455
 0.00038777937772768273
 0.00276141532345176117
 0.00056670689248167661
-0.00323852878331680298
-0.00193259906597622396
 0.00328731491441583683
 0.00362542589345449945
-0.00268785554870503091
-0.00545702740033716938
 0.00125317559909794512
 0.00713205061850966104
 0.00112492149537589880
-0.00826148096867550946
-0.00443085208574918715
 0.00839336065985300112
 0.00848839486761648020
-0.00705662837862476577
-0.01293781936476604867
 0.00380913247729417143
 0.01722862947876550518
 0.00172513205576642695
-0.02062091118580804822
-0.00985374287540894019
 0.02217056516274141381
 0.02089533076058044253
-0.02062760756367006815
-0.03546365160613443313
 0.01401630243169032369
 0.05541063386809867708
 0.00212045427486363437
-0.08794052708207862612
-0.04526389505656687462
 0.18221762668014460096
 0.41075960732889965632
 0.41075960732889965632
 0.18221762668014460096
-0.04526389505656687462
-0.08794052708207862612
 0.00212045427486363437
 0.05541063386809867708
 0.01401630243169032369
-0.03546365160613443313
-0.02062760756367006815
 0.02089533076058044253
 0.02217056516274141381
-0.00985374287540894019
-0.02062091118580804822
 0.00172513205576642695
 0.01722862947876550518
 0.00380913247729417143
-0.01293781936476604867
-0.00705662837862476577
 0.00848839486761648020
 0.00839336065985300112
-0.00443085208574918715
-0.00826148096867550946
 0.00112492149537589880
 0.00713205061850966104
 0.00125317559909794512
-0.00545702740033716938
-0.00268785554870503091
 0.00362542589345449945
 0.00328731491441583683
-0.00193259906597622396
-0.00323852878331680298
 0.00056670689248167661
 0.00276141532345176117
 0.00038777937772768273
-0.00206892788224323455
-0.00093286682198106608
 0.00133794323705832747
 0.00113148992142813524
-0.00069305675670084180
-0.00107928469499717137
 0.00020309303892565388
 0.00087909017443692499
 0.00011242294792251808
-0.00062153272838533420
-0.00027028256701905416
 0.00037363561338823163
 0.00030833229435342778
-0.00017513777576291543
-0.00027125558280978066
 0.00004097428652472217
 0.00019973378481126099
 0.00003231781361429016
-0.00012410666372671377
-0.00005884569707596701
 0.00006233931357689778
 0.00005623314602326363
-0.00002127235603321839
-0.00004038828866646377
-0.00000017886444625648
 0.00002272556429291851
 0.00000728628960094510
-0.00001008290833020705
-0.00000932875737718674
-0.00000152158394097619
'''.split())

fir_n = len(fir)

# would probably be faster to use numpy.convolve and slice, but oh well
def downsample(data):
	result = []
	buf = [0] * fir_n
	i = 0
	for x in data:
		buf[i] = x
		i = (i + 1) & (fir_n - 1)
		if (i & 1) == 0:
			y = 0
			for j in range(len(fir)):
				y += fir[j] * buf[(i + j) & (fir_n - 1)]
			result.append(y)
	return result

def wavwrite(seq, fn, sr = 44100):
  f = file(fn, 'wb')
  n_samples = len(seq)
  f.write(struct.pack('<4sI4s4sIHHIIHH4sI',
        'RIFF',
        36 + 2 * n_samples,
        'WAVE',
        'fmt ',
        16,
        1, 1,
        sr,
        2 * sr,
        2, 16,
        'data',
        2 * n_samples))
  for x in seq:
    f.write(struct.pack('<h', min(32767, max(-32767, int(16384 * x)))))

def main():
	parser = argparse.ArgumentParser()
	parser.add_argument("--oversample", help="oversample factor")
	parser.add_argument("--signal", help="saw or sinsweep")
	parser.add_argument("--k", help="resonance, 4 = oscillate")
	parser.add_argument("--filter", help="tpt, antti, or expm")
	parser.add_argument("--tanhfunc", help="tanh or invsqrt")
	parser.add_argument("--cutoff")
	parser.add_argument("--gain", help="gain in dB")
	parser.add_argument("--ogain", help="outputgain in dB")
	parser.add_argument("--out", help="output wav file")
	args = parser.parse_args()
	oversample = 1
	if args.oversample:
		oversample = int(args.oversample)
	k = 0
	if args.k:
		k = float(args.k)
	signal = 'saw'
	if args.signal:
		signal = args.signal
	global samplerate
	samplerate *= oversample
	n = 6 * samplerate
	if signal == 'saw':
		input = saw(n)
		aas = sweep(n)
	elif signal == 'sinsweep':
		input = sinsweep(n)
		aas = [1000./samplerate * 2 * pi] * n
	gain = 1
	if args.gain:
		gain = 10 ** (float(args.gain)/20)
	input = [y * gain for y in input]
	tanhfunc = tanh
	if args.tanhfunc == 'invsqrt':
		tanhfunc = invsqrt
	if args.filter == 'tpt':
		result = tpt_nl(input, aas, k)
	elif args.filter == 'antti':
		result = antti_nl(input, aas, k, tanhfunc = tanhfunc)
	else:
		result = expm_nl(input, aas, k, tanhfunc = tanhfunc)
	while oversample > 1:
		result = downsample(result)
		oversample /= 2
	ogain = 1
	if args.ogain:
		ogain = 10 ** (float(args.ogain)/20)
	if args.out:
		wavwrite(result, args.out)

main()
