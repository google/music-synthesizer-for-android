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
const int MAX_BUFFER_SIZE = 1024;
int buffer_size;

int16_t buffer[MAX_BUFFER_SIZE * N_BUFFERS];
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
static SLAndroidSimpleBufferQueueItf bq_player_buffer_queue;
static SLBufferQueueItf buffer_queue_itf;

extern "C" void BqPlayerCallback(SLAndroidSimpleBufferQueueItf queueItf,
  void *data) {
  if (count >= 1000) return;
  int16_t *buf_ptr = buffer + buffer_size * cur_buffer;
  synth_unit->GetSamples(buffer_size, buf_ptr);
  SLresult result = (*queueItf)->Enqueue(bq_player_buffer_queue,
    buf_ptr, buffer_size * 2);
  assert(SL_RESULT_SUCCESS == result);
  cur_buffer = (cur_buffer + 1) % N_BUFFERS;
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
  LOGI("engine started");
  }

extern "C" JNIEXPORT void JNICALL
Java_com_google_synthesizer_android_AndroidGlue_start(JNIEnv *env,
    jobject thiz, jint sample_rate, jint buf_size) {
  CreateEngine();
  SLDataLocator_AndroidSimpleBufferQueue loc_bufq =
    {SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, N_BUFFERS};
  SLDataFormat_PCM format_pcm = {
    SL_DATAFORMAT_PCM, 1, sample_rate * 1000,
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

  result = (*bq_player_buffer_queue)->RegisterCallback(bq_player_buffer_queue,
        &BqPlayerCallback, NULL);
  assert(SL_RESULT_SUCCESS == result);

  buffer_size = buf_size;
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
Java_com_google_synthesizer_android_AndroidGlue_shutdown(JNIEnv *env,
    jobject thiz) {
  LOGI("shutting down engine");
  if (bqPlayerObject != NULL) {
    (*bqPlayerObject)->Destroy(bqPlayerObject);
    bqPlayerObject = NULL;
    bq_player_play = NULL;
    bq_player_buffer_queue = NULL;
  }
  if (outputMixObject != NULL) {
    (*outputMixObject)->Destroy(outputMixObject);
    outputMixObject = NULL;
  }
  if (engineObject != NULL) {
    (*engineObject)->Destroy(engineObject);
    engineObject = NULL;
    engineEngine = NULL;
  }
  delete ring_buffer;
  ring_buffer = NULL;
  delete synth_unit;
  synth_unit = NULL;
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

