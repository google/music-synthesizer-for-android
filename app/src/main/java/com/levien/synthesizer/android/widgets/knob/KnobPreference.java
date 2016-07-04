/*
 * Copyright 2013 Google Inc.
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

package com.levien.synthesizer.android.widgets.knob;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;

import com.levien.synthesizer.R;

/**
 * A wrapper so that a knob can be used in the preference dialog
 */
public class KnobPreference extends Preference {

  public KnobPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
    // TODO Auto-generated constructor stub
  }

  @Override
  protected Object onGetDefaultValue(TypedArray a, int index) {
    return a.getFloat(index, 0.0f);
  }

  @Override
  protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
    setValue(restoreValue ? getPersistedFloat(value_) : (Float)defaultValue);
  }

  private void setValue(float value) {
    value_ = value;
    persistFloat(value);
  }

  @Override
  protected void onBindView(View view) {
    super.onBindView(view);

    KnobView knobView = (KnobView)view.findViewById(R.id.knob);
    knobView.setValue(value_);
    knobView.setKnobListenerUp(new KnobListener() {
      public void onKnobChanged(double newValue) {
        setValue((float)newValue);
      }
    });
  }

  private float value_;
}
