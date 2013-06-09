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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * MidiFile represents all of the midi information that could be read from a .mid or .smf file.
 * It contains a MidiHeader and 1 or more MidiTrack.
 */
public class MidiFile {
  /**
   * Creates a new empty MidiFile.
   */
  public MidiFile() {
    header_ = new MidiHeader();
    tracks_ = new ArrayList<MidiTrack>();
  }

  /**
   * Creates a new MidiFile from the data in InputStream.
   * @param input - The stream to read the data from.
   * @throws IOException - On any error reading the data.
   */
  public MidiFile(InputStream input) throws IOException {
    header_ = new MidiHeader();
    tracks_ = new ArrayList<MidiTrack>();
    MidiReader.readMidiFile(input, this);
  }

  /**
   * Returns a mutable header object for the file.
   */
  public MidiHeader getHeader() {
    return header_;
  }

  /**
   * Returns the number of tracks in this file.
   */
  public int getTrackCount() {
    return tracks_.size();
  }

  /**
   * Returns the mutable track at the given index.
   */
  public MidiTrack getTrack(int track) {
    return tracks_.get(track);
  }

  /**
   * Adds a new track to the file and returns a mutable reference to it.
   */
  public MidiTrack addTrack() {
    MidiTrack track = new MidiTrack();
    tracks_.add(track);
    return track;
  }

  // The header data.
  private MidiHeader header_;

  // The list of tracks.
  private List<MidiTrack> tracks_;
}
