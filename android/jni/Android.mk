LOCAL_PATH := $(call my-dir)/../../cpp/src

include $(CLEAR_VARS)
LOCAL_MODULE := synth
LOCAL_CPP_EXTENSION := .cc
LOCAL_SRC_FILES := android_glue.cc \
                   dx7note.cc \
                   env.cc \
                   exp2.cc \
                   fir.cc \
                   fm_core.cc \
                   fm_op_kernel.cc \
                   freqlut.cc \
                   lfo.cc \
                   patch.cc \
                   pitchenv.cc \
                   resofilter.cc \
                   ringbuffer.cc \
                   sawtooth.cc \
                   sin.cc \
                   synth_unit.cc

ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
    LOCAL_ARM_NEON := true
    LOCAL_CFLAGS := -DHAVE_NEON=1
    LOCAL_SRC_FILES += neon_fm_kernel.s \
                       neon_ladder.s \
                       neon_fir.s
endif

# for native audio
LOCAL_LDLIBS    += -lOpenSLES
# for logging
LOCAL_LDLIBS    += -llog

LOCAL_STATIC_LIBRARIES += cpufeatures

LOCAL_CFLAGS += -O3

include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_SRC_FILES := test_neon.cc \
  resofilter.cc

ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
    LOCAL_ARM_NEON := true
    LOCAL_CFLAGS := -DHAVE_NEON=1
    LOCAL_SRC_FILES += neon_fm_kernel.s \
                       neon_ladder.s \
                       neon_fir.s
endif

LOCAL_CFLAGS += -O3

LOCAL_STATIC_LIBRARIES += cpufeatures

LOCAL_MODULE := test_neon

include $(BUILD_EXECUTABLE)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := test_filter.cc \
  fir.cc

ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
    LOCAL_ARM_NEON := true
    LOCAL_CFLAGS := -DHAVE_NEON=1
    LOCAL_SRC_FILES += neon_fir.s \
                       neon_iir.s
endif

LOCAL_CFLAGS += -O3
LOCAL_STATIC_LIBRARIES += cpufeatures
LOCAL_MODULE := test_filter
include $(BUILD_EXECUTABLE)


$(call import-module,android/cpufeatures)
