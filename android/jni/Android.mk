LOCAL_PATH := $(call my-dir)/../../cpp/src

include $(CLEAR_VARS)
LOCAL_MODULE := synth
LOCAL_CPP_EXTENSION := .cc
LOCAL_SRC_FILES := android_glue.cc \
                   dx7note.cc \
                   env.cc \
                   fm_core.cc \
                   fm_op_kernel.cc \
                   freqlut.cc \
                   patch.cc \
                   resofilter.cc \
                   ringbuffer.cc \
                   sawtooth.cc \
                   sin.cc \
                   synth_unit.cc

# for native audio
LOCAL_LDLIBS    += -lOpenSLES
# for logging
LOCAL_LDLIBS    += -llog

LOCAL_CFLAGS := -O3

include $(BUILD_SHARED_LIBRARY)

