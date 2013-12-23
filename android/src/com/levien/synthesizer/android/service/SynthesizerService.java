/*
 * Copyright 2010 Google Inc.
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

package com.levien.synthesizer.android.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import com.levien.synthesizer.R;
import com.levien.synthesizer.android.AndroidGlue;
import com.levien.synthesizer.core.midi.MidiListener;
import com.levien.synthesizer.core.model.composite.MultiChannelSynthesizer;

/**
 * An Android Service that plays audio from a synthesizer.
 * The Service is created when the first Activity binds to it, and destroys itself when no more
 * Activities are bound to it.
 */
public class SynthesizerService extends Service {
  // Class for local client access.
  public class LocalBinder extends Binder {
    public SynthesizerService getService() {
      return SynthesizerService.this;
    }

    /**
     * Gets the underlying synthesizer powering this service.
     *
     * Obsolete, to be deleted.
     */
    public MultiChannelSynthesizer getSynthesizer() {
      return null;
    }
  }

  /**
   * Run when the Service is first created.
   */
  @Override
  public void onCreate() {
    Log.d("synth", "service onCreate");
    if (androidGlue_ == null) {
      AudioParams params = new AudioParams(44100, 64);
      // TODO: for pre-JB-MR1 devices, do some matching against known devices to
      // get best audio parameters.
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
        getJbMr1Params(params);
      }
      // Empirical testing shows better performance with small buffer size
      // than actually matching the media server's reported buffer size.
      params.bufferSize = 64;

      androidGlue_ = new AndroidGlue();
      androidGlue_.start(params.sampleRate, params.bufferSize);
      InputStream patchIs = getResources().openRawResource(R.raw.rom1a);
      byte[] patchData = new byte[4104];
      try {
        patchIs.read(patchData);
        androidGlue_.sendMidi(patchData);
        patchNames_ = new ArrayList<String>();
        for (int i = 0; i < 32; i++) {
          patchNames_.add(new String(patchData, 124 + 128 * i, 10, "ISO-8859-1"));
        }
      } catch (IOException e) {
        Log.e(getClass().getName(), "loading patches failed");
      }
    }
    androidGlue_.setPlayState(true);
  }

  /**
   * Run when this Service is finally destroyed.
   */
  @Override
  public void onDestroy() {
    androidGlue_.setPlayState(false);
  }

  /**
   * Run when an Activity binds to this Service.
   */
  @Override
  public IBinder onBind(Intent intent) {
    return binder_;
  }

  public MidiListener getMidiListener() {
    return androidGlue_;
  }

  /**
   * Sends raw MIDI data to the synthesizer.
   *
   * @param buf MIDI bytes to send
   */
  public void sendRawMidi(byte[] buf) {
    androidGlue_.sendMidi(buf);
  }

  public List<String> getPatchNames() {
    return patchNames_;
  }

  class AudioParams {
    AudioParams(int sr, int bs) {
      confident = false;
      sampleRate = sr;
      bufferSize = bs;
    }
    public String toString() {
      return "sampleRate=" + sampleRate + " bufferSize=" + bufferSize;
    }
    boolean confident;
    int sampleRate;
    int bufferSize;
  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
  void getJbMr1Params(AudioParams params) {
      AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
    String sr = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
    String bs = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
    params.confident = true;
    params.sampleRate = Integer.parseInt(sr);
    params.bufferSize = Integer.parseInt(bs);
  }

  // Binder to use for Activities in this process.
  private final IBinder binder_ = new LocalBinder();

  private static AndroidGlue androidGlue_;

  private static List<String> patchNames_;
}
