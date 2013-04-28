package com.google.synthesizer.android.stats;

import android.util.Log;

public class JitterStats {
  public JitterStats() {
    startTime_ = new double[N_STATS];
    endTime_ = new double[N_STATS];

  }

  // Takes stats buf in format from android_glue
  public void aggregate(byte[] statsBuf) {
    String[] lines = new String(statsBuf).split("\n");
    for (int i = 0; i < lines.length; i++) {
      String[] toks = lines[i].split(" ");
      if (toks.length == 3 && "ts".equals(toks[0])) {
        double startTime = Double.parseDouble(toks[1]);
        double endTime = Double.parseDouble(toks[2]);
        startTime_[bufIx_] = startTime;
        endTime_[bufIx_] = endTime;
        bufIx_ = (bufIx_ + 1) % N_STATS;
        double cbTime = endTime - startTime;
        meanCbTime_ += (cbTime - meanCbTime_) * .01;
      }
    }
  }

  public String report() {
    double maxCbTime = 0.0;
    for (int i = 0; i < N_STATS; i++) {
      double cbTime = endTime_[i] - startTime_[i];
      maxCbTime = Math.max(maxCbTime, cbTime);
    }
    return "max cb = " + Double.toString(maxCbTime * 1000) + "ms";
  }

  static final int N_STATS = 2000;
  double meanCbTime_;
  double startTime_[];
  double endTime_[];
  int bufIx_ = 0;

}
