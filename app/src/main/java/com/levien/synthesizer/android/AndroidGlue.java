package com.levien.synthesizer.android;

import com.levien.synthesizer.core.midi.MessageOutputProcessor;

/**
 * JNI container for connecting to C++ synth engine. The actual implementation is in the cpp/src
 * subdirectory of the repository, and interfaces with JNI.
 * 
 * This class implements the MessageOutputProcessor interface, so you can use those methods to
 * actually send MIDI data.
 */
public class AndroidGlue extends MessageOutputProcessor {

  /**
   * Create and initialize the engine. This should be done once per process.
   */
  public native void start(int sample_rate, int buf_size);

  /**
   *  Shut down the OpenSL ES engine and audio synthesizer.
   */
  public native void shutdown();
  
  /**
   * Start or pause the actual sound generation.
   * 
   * @param isPlaying Whether the sound generation should be enabled or no.
   */
  public native void setPlayState(boolean isPlaying);

  /**
   * Send a MIDI message. Currently supported messages include DX7 sysex data, and note-on/note-off,
   * but it will expand.
   * 
   * @param midiData The midi data to send.
   */
  public native void sendMidi(byte[] midiData);

  public void onMessage(byte[] midiData) {
    sendMidi(midiData);
  }

  /**
   * @return Number of stats bytes available from synth core
   */
  public native int statsBytesAvailable();

  public native int readStatsBytes(byte[] buf, int off, int len);

  static {
    System.loadLibrary("synth");
  }
}
