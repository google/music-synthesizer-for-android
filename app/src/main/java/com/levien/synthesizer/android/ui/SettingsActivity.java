package com.levien.synthesizer.android.ui;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;

import com.levien.synthesizer.R;

public class SettingsActivity extends PreferenceActivity {
  @SuppressWarnings("deprecation")
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.preferences);
    ListPreference keyboardTypePref = (ListPreference)findPreference("keyboard_type");
    updateListSummary(keyboardTypePref, keyboardTypePref.getValue());
    keyboardTypePref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
      public boolean onPreferenceChange(Preference pref, Object newVal) {
        updateListSummary(pref, newVal.toString());
        return true;
      }
    });
  }

  private void updateListSummary(Preference pref, String newVal) {
    ListPreference lp = (ListPreference)pref;
    int index = lp.findIndexOfValue(newVal);
    lp.setSummary(lp.getEntries()[index]);
  }
}

