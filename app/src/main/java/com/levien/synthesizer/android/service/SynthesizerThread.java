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

// This class is probably obsolete and should be deleted

package com.levien.synthesizer.android.service;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

/**
 * SynthesizerThread is a thread-safe interface to a thread that constantly plays sampled audio data
 * from a given SignalProvider.
 */
public class SynthesizerThread {
  /**
   * Creates a new SynthesizerThread that will play audio from synthesizer.
   */
  public SynthesizerThread(int sampleRateInHz) {
    playingLock_ = new Object();
    playing_ = false;
    shouldDie_ = false;
    audioTrackLock_ = new Object();
    audioTrack_ = null;
    sampleRateInHz_ = sampleRateInHz;
  }

  /**
   * Starts the thread taking sampled audio data from the synthesizer and playing it.
   */
  public void play() {
    synchronized (playingLock_) {
      if (playing_) {
        // It's already playing.
        return;
      }

      // Set up the initial state.
      playing_ = true;
      shouldDie_ = false;
      initSound();
      fillBuffer();

      // Start the audio track playing from the buffer.
      startSound();

      // Start the thread that grabs the output from the synthesizer and fills the output buffer.
      speakerThread_ = new Thread(new Runnable() {
        public void run() {
          while (true) {
            synchronized (playingLock_) {
              // Check if the thread should die.
              if (shouldDie_) {
                // This thread has been signalled to stop.
                Log.i(getClass().getName(), "Dying now.");
                playing_ = false;
                shouldDie_ = false;
                cleanupSound();
                return;
              }
            }
            // Do the actual work.
            fillBuffer();
          }
        }
      });
      speakerThread_.setName(getClass().getName());
      speakerThread_.setPriority(Thread.MAX_PRIORITY);
      speakerThread_.setDaemon(true);
      speakerThread_.start();
    }
  }

  /**
   * Tells the thread it should stop getting sampled data and outputting it as soon as possible.
   */
  public void stop() {
    Log.i(getClass().getName(), "stop() is waiting for playingLock_.");
    synchronized (playingLock_) {
      Log.i(getClass().getName(), "stop() received.");
      shouldDie_ = true;
    }
  }

  /**
   * Blocks until the thread is no longer playing.
   * You better call stop() before calling this.
   */
  public void waitForStop() {
    while (true) {
      synchronized (playingLock_) {
        if (!playing_) {
          return;
        }
      }
    }
  }

  /**
   * Initializes the data structures needed for playing.
   */
  private void initSound() {
    synchronized (audioTrackLock_) {
      if (audioTrack_ != null) {
        cleanupSound();
      }

      // Get the smallest buffer to minimize latency.
      int bufferSizeInBytes = AudioTrack.getMinBufferSize(sampleRateInHz_,
                                                          AudioFormat.CHANNEL_CONFIGURATION_MONO,
                                                          AudioFormat.ENCODING_PCM_16BIT);
      Log.i(getClass().getName(), "Output sample rate: " + sampleRateInHz_);
      Log.i(getClass().getName(), "Minimum buffer size: " + bufferSizeInBytes);
      Log.i(getClass().getName(), "Minimum volume: " + AudioTrack.getMinVolume());
      Log.i(getClass().getName(), "Maximum volume: " + AudioTrack.getMaxVolume());

      audioTrack_ = new AudioTrack(
          AudioManager.STREAM_MUSIC,  // int streamType,
          sampleRateInHz_,  // int sampleRateInHz,
          AudioFormat.CHANNEL_OUT_MONO,  // int channelConfig,
          AudioFormat.ENCODING_PCM_16BIT,  // int audioFormat,
          bufferSizeInBytes,  // int bufferSizeInBytes,
          AudioTrack.MODE_STREAM); // int mode);
      buffer_ = new short[bufferSizeInBytes / 8];
    }
  }

  /**
   *  Starts the AudioTrack playing.
   */
  private void startSound() {
    synchronized (audioTrackLock_) {
      if (audioTrack_ != null) {
        audioTrack_.play();
      }
    }
  }

  /**
   * Cleans up the internal data structures.
   */
  private void cleanupSound() {
    synchronized (audioTrackLock_) {
      if (audioTrack_ != null) {
        audioTrack_.stop();
        audioTrack_.release();
        audioTrack_ = null;
        buffer_ = null;
      }
    }
  }

  /**
   * Fills the buffer once, and then sends it off to the AudioTrack.
   */
  private void fillBuffer() {
    for (int i = 0; i < buffer_.length; ++i) {
      // Change the output range from [-1, 1] to [-32767, 32767].
      // 16-bit signed output is fairly standard, and hard-coded.
      double output = 0;
      // Clamp values out of range.
      if (output < -1.0) {
        output = -1.0;
      }
      if (output > 1.0) {
        output = 1.0;
      }
      buffer_[i] = (short)(32767 * output);
    }
    synchronized (audioTrackLock_) {
      if (audioTrack_ != null) {
        // This call will block until the buffer has been copied, but will return before the
        // sampled audio data has actually been output.
        audioTrack_.write(buffer_, 0, buffer_.length);
      }
    }
  }

  // The actual thread that constantly loops, taking the output from the synthesizer and sending it
  // to Android's audio output buffer.
  Thread speakerThread_;

  // Lock guarding playing_, shouldDie_, and speakerThread_.
  private Object playingLock_;

  // True iff the thread is in the "playing" state.
  private boolean playing_;

  // Variable used to signal to the speakerThread that playing should stop.
  private boolean shouldDie_;

  // Lock guarding audioTrack_, buffer_, and time_.
  private Object audioTrackLock_;

  // Android object for outputting audio.
  private AudioTrack audioTrack_;

  // Buffer for collecting output from the synthesizer until it is ready to be output.
  private short[] buffer_;

  // The sample rate of the synthesizer.
  private int sampleRateInHz_;
}
