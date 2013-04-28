LOCAL_PATH := $(call my-dir)/../../cpp/src

include $(CLEAR_VARS)
LOCAL_MODULE := synth
LOCAL_CPP_EXTENSION := .cc
LOCAL_SRC_FILES := android_glue.cc \
                   dx7note.cc \
                   env.cc \
                   exp2.cc \
                   fm_core.cc \
                   fm_op_kernel.cc \
                   freqlut.cc \
                   patch.cc \
                   resofilter.cc \
                   ringbuffer.cc \
                   sawtooth.cc \
                   sin.cc \
                   synth_unit.cc

ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
    LOCAL_ARM_NEON := true
    LOCAL_CFLAGS := -DHAVE_NEON=1
    LOCAL_SRC_FILES += neon_fm_kernel.s
endif

# for native audio
LOCAL_LDLIBS    += -lOpenSLES
# for logging
LOCAL_LDLIBS    += -llog

LOCAL_STATIC_LIBRARIES += cpufeatures

LOCAL_CFLAGS += -O3

include $(BUILD_SHARED_LIBRARY)

$(call import-module,android/cpufeatures)