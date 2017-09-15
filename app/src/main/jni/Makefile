# Makefile for emscripten

SRC := dx7note.cc env.cc exp2.cc fm_core.cc fm_op_kernel.cc freqlut.cc \
	lfo.cc log2.cc patch.cc pitchenv.cc resofilter.cc ringbuffer.cc \
	sawtooth.cc sin.cc synth_unit.cc emscripten.cc

EXP := "['_synth_create','_synth_get_samples','_synth_send_midi']"

synthcore.js: $(SRC)
	em++ $^ -o $@ -s EXPORTED_FUNCTIONS=$(EXP) -O2
