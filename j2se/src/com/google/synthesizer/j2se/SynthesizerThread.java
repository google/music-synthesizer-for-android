/*
 * Copyright 2011 Google Inc.
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

package com.google.synthesizer.j2se;

import com.google.synthesizer.core.model.SignalProvider;
import com.google.synthesizer.core.model.SynthesisTime;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

/**
 * SynthesizerThread is a thread-safe interface to a thread that constantly plays sampled audio data
 * from a given SignalProvider.
 */
public class SynthesizerThread {
  /**
   * Creates a new SynthesizerThread that will play audio from synthesizer.
   * @param synthesizer - the source of the audio data to output.
   */
  public SynthesizerThread(SignalProvider synthesizer, int sampleRateInHz) {
    playingLock_ = new Object();
    playing_ = false;
    shouldDie_ = false;
    sourceDataLineLock_ = new Object();
    sourceDataLine_ = null;
    synthesizer_ = synthesizer;
    sampleRateInHz_ = sampleRateInHz;
    time_ = new SynthesisTime();
    time_.setSampleRate(sampleRateInHz);
    logger_ = Logger.getLogger(getClass().getName());
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
                logger_.info("Dying now.");
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
    logger_.info("stop() is waiting for playingLock_.");
    synchronized (playingLock_) {
      logger_.info("stop() received.");
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
    synchronized (sourceDataLineLock_) {
      if (sourceDataLine_ != null) {
        cleanupSound();
      }

      StringBuilder debug = new StringBuilder("\n");
      Mixer.Info[] mixers = AudioSystem.getMixerInfo();
      for (int i = 0; i < mixers.length; ++i) {
        debug.append("Mixer " + i + ": " +
                     mixers[i].getName() + ": " + mixers[i].getDescription() + "\n");
        Line.Info[] source_lines = AudioSystem.getMixer(mixers[i]).getSourceLineInfo();
        for (int j = 0; j < source_lines.length; ++j) {
          debug.append("  Source Line " + j + ": " + source_lines[j].toString() + "\n");
          if (source_lines[j] instanceof DataLine.Info) {
            DataLine.Info dataLineInfo = (DataLine.Info)source_lines[j];
            AudioFormat[] supportedFormats = dataLineInfo.getFormats();
            for (int k = 0; k < supportedFormats.length; k++) {
              debug.append("    Format: " + supportedFormats[k].toString() + "\n");
            }
          }
        }
        Line.Info[] targetLines = AudioSystem.getMixer(mixers[i]).getTargetLineInfo();
        for (int j = 0; j < targetLines.length; ++j) {
          debug.append("  Target Line " + j + ": " + targetLines[j].toString() + "\n");
        }
      }
      logger_.info(debug.toString());

      int bufferSizeInBytes = (int)(DELAY * sampleRateInHz_);

      AudioFormat format = new AudioFormat(sampleRateInHz_, 16, 1, true, false);
      try {
        sourceDataLine_ = AudioSystem.getSourceDataLine(format);
        sourceDataLine_.open(format, bufferSizeInBytes);
      } catch (LineUnavailableException e) {
        logger_.log(Level.SEVERE, "Unable to open SourceDataLine.", e);
      }

      buffer_ = new byte[(bufferSizeInBytes / 8) * 2];
      time_.reset();
    }
  }

  /**
   *  Starts the AudioTrack playing.
   */
  private void startSound() {
    synchronized (sourceDataLineLock_) {
      if (sourceDataLine_ != null) {
        sourceDataLine_.start();
      }
    }
  }

  /**
   * Cleans up the internal data structures.
   */
  private void cleanupSound() {
    synchronized (sourceDataLineLock_) {
      if (sourceDataLine_ != null) {
        sourceDataLine_.drain();
        sourceDataLine_.stop();
        sourceDataLine_.close();
        sourceDataLine_ = null;
        buffer_ = null;
      }
    }
  }

  /**
   * Fills the buffer once, and then sends it off to the AudioTrack.
   */
  private void fillBuffer() {
    for (int i = 0; i < buffer_.length; i += 2) {
      // Change the output range from [-1, 1] to [-32767, 32767].
      // 16-bit signed output is fairly standard, and hard-coded.
      double output = 32 * synthesizer_.getValue(time_);
      // Clamp values out of range.
      if (output < -1.0) {
        output = -1.0;
      }
      if (output > 1.0) {
        output = 1.0;
      }
      short shortOutput = (short)(32767 * output);
      buffer_[i] = (byte)(shortOutput & 0xFF);
      buffer_[i + 1] = (byte)((shortOutput >> 8) & 0xFF);
      time_.advance();
    }
    synchronized (sourceDataLineLock_) {
      if (sourceDataLine_ != null) {
        // This call will block until the buffer has been copied, but will return before the
        // sampled audio data has actually been output.
        sourceDataLine_.write(buffer_, 0, buffer_.length);
      }
    }
  }

  // The actual thread that constantly loops, taking the output from the synthesizer and sending it
  // to the audio output buffer.
  private Thread speakerThread_;

  // Lock guarding playing_, shouldDie_, and speakerThread_.
  private Object playingLock_;

  // True iff the thread is in the "playing" state.
  private boolean playing_;

  // Variable used to signal to the speakerThread that playing should stop.
  private boolean shouldDie_;

  // Lock guarding sourceDataLine_, buffer_, and time_.
  private Object sourceDataLineLock_;

  // J2SE object for outputting sound.
  private SourceDataLine sourceDataLine_;

  // Buffer for collecting output from the synthesizer until it is ready to be output.
  private byte[] buffer_;

  // The sample rate of the synthesizer.
  private int sampleRateInHz_;

  // Tracker for time since synthesis started.
  private SynthesisTime time_;

  // Module to provide sampled audio data to be output.
  private SignalProvider synthesizer_;

  // Object for logging.
  private static Logger logger_;

  // Delay in seconds between controlling the synth and hearing output.
  private static final double DELAY = 0.1;
}
