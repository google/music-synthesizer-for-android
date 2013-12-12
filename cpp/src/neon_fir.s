@ Copyright 2013 Google Inc.
@ 
@ Licensed under the Apache License, Version 2.0 (the "License");
@ you may not use this file except in compliance with the License.
@ You may obtain a copy of the License at
@ 
@      http://www.apache.org/licenses/LICENSE-2.0
@ 
@ Unless required by applicable law or agreed to in writing, software
@ distributed under the License is distributed on an "AS IS" BASIS,
@ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@ See the License for the specific language governing permissions and
@ limitations under the License.

@ NEON assembly implementation of FIR filter core

	.text

	.align 2
	.global neon_fir_direct
	.type neon_fir_direct, %function
neon_fir_direct:
@ r0 = pointer to input (aligned)
@ r1 = pointer to kernel (aligned)
@ r2 = pointer to output (aligned)
@ r3  = size of input in floats (multiple of 4)
@ stack[0] = size of kernel in floats (multiple of 4)

	push {r4-r7}
	ldr r4, [sp, #(4 * 4)]

	lsl r6, r4, #2
	sub r6, #16

	@ compute initial overlap
	mov r7, r4
	vmov.i32 q9, #0
	vmov.i32 q10, #0
	vmov.i32 q11, #0
neon_fir_direct1:
	vld1.i32 {q0}, [r0:128]!  @ load 4 samples from input
	vld1.i32 {q1}, [r1:128]!  @ load 4 samples from kernel
	vmla.f32 q9, q1, d0[1]
	vmla.f32 q10, q1, d1[0]
	vmla.f32 q11, q1, d1[1]
	subs r7, #4
	bne neon_fir_direct1
	vmov.i32 q12, #0
	vext.32 q13, q9, q12, #3
	vext.32 q1, q10, q12, #2
	vadd.f32 q13, q1
	vext.32 q1, q11, q12, #1
	vadd.f32 q8, q13, q1
	sub r0, r6
	sub r1, r4, lsl #2

neon_fir_direct2:

	mov r7, r4
	vmov.i32 q9, #0
	vmov.i32 q10, #0
	vmov.i32 q11, #0
	@ inner loop
neon_fir_direct3:
	vld1.32 {q0}, [r0:128]!  @ load 4 samples from input
	vld1.i32 {q1}, [r1:128]!  @ load 4 samples from kernel
	vmla.f32 q9, q1, d0[1]
	vmla.f32 q8, q1, d0[0]
	vmla.f32 q10, q1, d1[0]
	vmla.f32 q11, q1, d1[1]
	subs r7, #4
	bne neon_fir_direct3

	@ process overlaps
	vext.32 q0, q12, q9, #3
	vext.32 q13, q9, q12, #3
	vadd.f32 q8, q0
	vext.32 q0, q12, q10, #2
	vext.32 q1, q10, q12, #2
	vadd.f32 q8, q0
	vadd.f32 q13, q1
	vext.32 q0, q12, q11, #1
	vext.32 q1, q11, q12, #1
	vadd.f32 q0, q8
	vadd.f32 q8, q13, q1

	sub r0, r6
	sub r1, r4, lsl #2
	vst1.32 {q0}, [r2:128]!

	subs r3, #4
	bne neon_fir_direct2

	pop {r4-r7}
	bx lr

	.size neon_fir_direct, .-neon_fir_direct
