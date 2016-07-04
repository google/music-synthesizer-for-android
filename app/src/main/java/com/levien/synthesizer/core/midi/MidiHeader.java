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

package com.levien.synthesizer.core.midi;

import java.util.EnumSet;

/**
 * MidiHeader is a container for the information in the header of a .mid or .smf file.
 */
public class MidiHeader {
  /**
   * An enumeration of the kinds of formats a midi file can have, along with the number that
   * represents that format in the serialized header.
   */
  public enum Format {
    SINGLE_TRACK (0),
    MULTIPLE_TRACKS_SYNCH (1),
    MULTIPLE_TRACKS_ASYNCH (2);

    Format(int index) {
      index_ = index;
    }

    /**
     * Converts the integer value you would see in the file's header into the enum value.
     */
    public static Format valueOf(int index) {
      for (Format format : EnumSet.allOf(Format.class)) {
        if (format.index_ == index) {
          return format;
        }
      }
      throw new ArrayIndexOutOfBoundsException(index);
    }

    private int index_;
  }

  /**
   * Creates an empty header.
   */
  public MidiHeader() {
  }

  /**
   * @return The format of the file.
   */
  public Format getFormat() {
    return format_;
  }

  /**
   * @return The number of tracks in the file.
   */
  public int getTrackCount() {
    return trackCount_;
  }

  /**
   * The number of midi event ticks per "beat", along with the tempo in beats-per-minute, allows
   * mapping the ticks into real time.  The tempo may change from onSetTempo events, but this value
   * will stay constant.
   * @return The number of midi event ticks per "beat".
   */
  public int getTicksPerBeat() {
    return ticksPerBeat_;
  }

  /**
   * TODO(klimt): Unused until SMPTE time information is supported.
   */
  public int getFramesPerSecond() {
    return framesPerSecond_;
  }

  /**
   * TODO(klimt): Unused until SMPTE time information is supported.
   */
  public int getTicksPerFrame() {
    return ticksPerFrame_;
  }

  /**
   * Sets the format of the file.
   */
  public void setFormat(Format format) {
    format_ = format;
  }

  /**
   * Sets the number of tracks in the file.
   */
  public void setTrackCount(int trackCount) {
    trackCount_ = trackCount;
  }

  /**
   * Sets the number of ticks per beat for the file.
   */
  public void setTicksPerBeat(int ticksPerBeat) {
    ticksPerBeat_ = ticksPerBeat;
  }

  /**
   * TODO(klimt): Unused until SMPTE time information is supported.
   */
  public void setFramesPerSecond(int framesPerSecond) {
    framesPerSecond_ = framesPerSecond;
  }

  /**
   * TODO(klimt): Unused until SMPTE time information is supported.
   */
  public void setTicksPerFrame(int ticksPerFrame) {
    ticksPerFrame_ = ticksPerFrame;
  }

  private Format format_;
  private int trackCount_;
  private int ticksPerBeat_;
  private int framesPerSecond_;
  private int ticksPerFrame_;
}
