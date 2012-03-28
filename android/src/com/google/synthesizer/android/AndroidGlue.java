package com.google.synthesizer.android;


public class AndroidGlue {

  public native void start();
  public native void setPlayState(boolean isPlaying);
  public native void sendMidi(byte[] midiData);

  static {
    System.loadLibrary("synth");
  }
}

