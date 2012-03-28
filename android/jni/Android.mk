LOCAL_PATH := $(call my-dir)/../../cpp/src

include $(CLEAR_VARS)
LOCAL_MODULE := synth
LOCAL_CPP_EXTENSION := .cc
LOCAL_SRC_FILES := dx7note.cc \
                   env.cc \
                   fm_core.cc \
                   fm_op_kernel.cc \
                   freqlut.cc \
                   resofilter.cc \
                   ringbuffer.cc \
                   sawtooth.cc \
                   sin.cc \
                   synth_unit.cc

include $(BUILD_SHARED_LIBRARY)

