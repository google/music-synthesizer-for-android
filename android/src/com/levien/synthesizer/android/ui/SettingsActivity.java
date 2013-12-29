package com.levien.synthesizer.android.ui;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.levien.synthesizer.R;

public class SettingsActivity extends PreferenceActivity {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.preferences);
  }
}

