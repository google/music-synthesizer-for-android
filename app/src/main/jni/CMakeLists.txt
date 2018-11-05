cmake_minimum_required(VERSION 3.6.0)

file ( GLOB sources
	android_glue.cc
	dx7note.cc
	env.cc
	exp2.cc
	fir.cc
	fm_core.cc
	fm_op_kernel.cc
	freqlut.cc
	lfo.cc
	log2.cc
	patch.cc
	pitchenv.cc
	resofilter.cc
	ringbuffer.cc
	sawtooth.cc
	sin.cc
	synth_unit.cc )

add_library ( synth SHARED ${sources} )
target_link_libraries ( synth log OpenSLES )
