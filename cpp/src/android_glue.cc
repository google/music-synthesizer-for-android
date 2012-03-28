#include <assert.h>
#include <stddef.h>
#include <jni.h>
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>

#include <android/log.h>
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "synth", __VA_ARGS__)

#include "synth.h"
#include "freqlut.h"
#include "sin.h"
#include "synth_unit.h"

RingBuffer *ring_buffer;
SynthUnit *synth_unit;

const int N_BUFFERS = 2;
const int BUFFER_SIZE = 64;

int16_t buffer[BUFFER_SIZE * N_BUFFERS];
int cur_buffer = 0;
int count = 0;

// engine interfaces
static SLObjectItf engineObject = NULL;
static SLEngineItf engineEngine;

// output mix interfaces
static SLObjectItf outputMixObject = NULL;

// buffer queue player interfaces
static SLObjectItf bqPlayerObject = NULL;
static SLPlayItf bq_player_play;
static SLVolumeItf bq_player_volume;
static SLAndroidSimpleBufferQueueItf bq_player_buffer_queue;
static SLBufferQueueItf buffer_queue_itf;

extern "C" void BqPlayerCallback(SLAndroidSimpleBufferQueueItf queueItf,
    void *data) {
  if (count >= 1000) return;
  int16_t *buf_ptr = buffer + BUFFER_SIZE * cur_buffer;
  synth_unit->GetSamples(BUFFER_SIZE, buf_ptr);
  SLresult result = (*queueItf)->Enqueue(bq_player_buffer_queue,
      buf_ptr, BUFFER_SIZE * 2);
  assert(SL_RESULT_SUCCESS == result);
  cur_buffer = (cur_buffer + 1) % N_BUFFERS;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_google_synthesizer_android_AndroidGlue_hello(JNIEnv *env,
    jobject thiz) {
  LOGI("here %d!", 42);
  return 42;
}

void CreateEngine() {
  SLresult result;
  result = slCreateEngine(&engineObject, 0, NULL, 0, NULL, NULL);
  assert(SL_RESULT_SUCCESS == result);

  result = (*engineObject)->Realize(engineObject, SL_BOOLEAN_FALSE);
  assert(SL_RESULT_SUCCESS == result);

  result = (*engineObject)->GetInterface(engineObject, SL_IID_ENGINE,
      &engineEngine);
  assert(SL_RESULT_SUCCESS == result);

  result = (*engineEngine)->CreateOutputMix(engineEngine, &outputMixObject,
      0, NULL, NULL);
  assert(SL_RESULT_SUCCESS == result);
  result = (*outputMixObject)->Realize(outputMixObject, SL_BOOLEAN_FALSE);
  assert(SL_RESULT_SUCCESS == result);
}

extern "C" JNIEXPORT void JNICALL
Java_com_google_synthesizer_android_AndroidGlue_start(JNIEnv *env,
    jobject thiz) {
  CreateEngine();
  SLDataLocator_AndroidSimpleBufferQueue loc_bufq =
    {SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, N_BUFFERS};
  SLDataFormat_PCM format_pcm = {
    SL_DATAFORMAT_PCM, 1, SL_SAMPLINGRATE_48,
    SL_PCMSAMPLEFORMAT_FIXED_16, SL_PCMSAMPLEFORMAT_FIXED_16,
    SL_SPEAKER_FRONT_CENTER, SL_BYTEORDER_LITTLEENDIAN
      // TODO: compute real endianness
  };
  SLDataSource audio_src = {&loc_bufq, &format_pcm};
  SLDataLocator_OutputMix loc_outmix = {SL_DATALOCATOR_OUTPUTMIX,
    outputMixObject};
  SLDataSink audio_sink = {&loc_outmix, NULL};
  const SLInterfaceID ids[2] = {SL_IID_BUFFERQUEUE, SL_IID_VOLUME};
  const SLboolean req[2] = {SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE};
  SLresult result;
  result = (*engineEngine)->CreateAudioPlayer(engineEngine, &bqPlayerObject,
      &audio_src, &audio_sink, 2, ids, req);
  assert(SL_RESULT_SUCCESS == result);
  result = (*bqPlayerObject)->Realize(bqPlayerObject, SL_BOOLEAN_FALSE);
  assert(SL_RESULT_SUCCESS == result);
  result = (*bqPlayerObject)->GetInterface(bqPlayerObject, SL_IID_PLAY,
      &bq_player_play);
  assert(SL_RESULT_SUCCESS == result);
  result = (*bqPlayerObject)->GetInterface(bqPlayerObject, SL_IID_BUFFERQUEUE,
      &bq_player_buffer_queue);
  assert(SL_RESULT_SUCCESS == result);

  result = (*bqPlayerObject)->GetInterface(bqPlayerObject, SL_IID_VOLUME,
      &bq_player_volume);
  assert(SL_RESULT_SUCCESS == result);
  result = (*bq_player_buffer_queue)->RegisterCallback(bq_player_buffer_queue,
      &BqPlayerCallback, NULL);
  assert(SL_RESULT_SUCCESS == result);

  double sample_rate = 48000.0;
  Freqlut::init(sample_rate);
  Sin::init();
  ring_buffer = new RingBuffer();
  synth_unit = new SynthUnit(ring_buffer);
  for (int i = 0; i < N_BUFFERS - 1; ++i) {
    BqPlayerCallback(bq_player_buffer_queue, NULL);
  }

  result = (*bq_player_play)->SetPlayState(bq_player_play,
      SL_PLAYSTATE_PLAYING);
  assert(SL_RESULT_SUCCESS == result);
}

extern "C" JNIEXPORT void JNICALL
Java_com_google_synthesizer_android_AndroidGlue_sendMidi(JNIEnv *env,
    jobject thiz, jbyteArray jb) {
  uint8_t *data = (uint8_t *)env->GetByteArrayElements(jb, NULL);
  if (data != NULL) {
    ring_buffer->Write(data, env->GetArrayLength(jb));
    env->ReleaseByteArrayElements(jb, (jbyte *)data, JNI_ABORT);
  }
}

extern "C" JNIEXPORT void JNICALL
Java_com_google_synthesizer_android_AndroidGlue_setPlayState(JNIEnv *env,
    jobject thiz, jboolean isPlaying) {
  SLresult result = (*bq_player_play)->SetPlayState(bq_player_play,
      isPlaying ? SL_PLAYSTATE_PLAYING : SL_PLAYSTATE_PAUSED);
  assert(SL_RESULT_SUCCESS == result);
}

