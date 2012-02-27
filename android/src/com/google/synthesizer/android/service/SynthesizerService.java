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

package com.google.synthesizer.android.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.google.synthesizer.R;
import com.google.synthesizer.core.model.composite.MultiChannelSynthesizer;
import com.google.synthesizer.core.soundfont.SoundFontReader;

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
     */
    public MultiChannelSynthesizer getSynthesizer() {
      return SynthesizerService.this.synthesizer_;
    }
  }

  public SynthesizerService() {
    logger_ = Logger.getLogger(getClass().getName());
  }

  /**
   * Run when the Service is first created.
   */
  @Override
  public void onCreate() {
    super.onCreate();

    // Get the native audio settings.
    int sampleRateInHz = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC);
    // For now, cap the sample rate to reduce cpu requirements.
    sampleRateInHz = Math.min(sampleRateInHz, 11025);
    SoundFontReader sampleLibrary = null;
    InputStream sampleLibraryFile = getResources().openRawResource(R.raw.drums);
    try {
      sampleLibrary = new SoundFontReader(sampleLibraryFile);
    } catch (IOException e) {
      logger_.log(Level.SEVERE, "Unable to load sample library.", e);
      sampleLibrary = null;
    }
    synthesizer_ = new MultiChannelSynthesizer(CHANNELS, FINGERS, sampleRateInHz, sampleLibrary);

    // Load the presets from a file.
    InputStream presetsFile = getResources().openRawResource(R.raw.presets);
    try {
      synthesizer_.loadLibraryFromText(presetsFile);
    } catch (IOException e) {
      Log.e(getClass().getName(), "Unable to load presets from raw file.", e);
    }

    // Start a synthsizer thread playing the data.
    synthesizerThread_ = new SynthesizerThread(synthesizer_, sampleRateInHz);
    synthesizerThread_.play();

    // No Activities are yet bound to this Service.
    referenceCount_ = 0;
  }

  /**
   * Run when this Service is finally destroyed.
   */
  @Override
  public void onDestroy() {
    super.onDestroy();

    // Free up the underlying data structures.
    synthesizerThread_.stop();
    synthesizerThread_ = null;
    synthesizer_ = null;
  }

  /**
   * Run when an Activity binds to this Service.
   */
  @Override
  public IBinder onBind(Intent intent) {
    ++referenceCount_;
    return binder_;
  }

  /**
   * Run when any Activity unbinds from this Service.
   */
  @Override
  public boolean onUnbind(Intent intent) {
    if (--referenceCount_ == 0) {
      // No more Activities are using this Service, so kill it.
      stopSelf();
    }
    return super.onUnbind(intent);
  }

  // Binder to use for Activities in this process.
  private final IBinder binder_ = new LocalBinder();

  // The module that provides the sampled audio data.
  private MultiChannelSynthesizer synthesizer_;

  // The thread that actually does the work of playing the audio data.
  private SynthesizerThread synthesizerThread_;

  // How many Activities are currently bound to this Service.
  private int referenceCount_;

  // The number of channels the synthesizer supports.
  private static final int CHANNELS = 8;

  // The number of fingers the synthesizer supports.
  private static final int FINGERS = 5;

  private Logger logger_;
}
