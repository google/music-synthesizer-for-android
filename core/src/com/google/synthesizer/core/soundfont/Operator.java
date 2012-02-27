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

package com.google.synthesizer.core.soundfont;

/**
 * An enumeration of all possible SoundFont Generator Operators.
 * Most of these are ignored.
 */
public enum Operator {
  UNSUPPORTED(-1),
  START_ADDRS_OFFSET(0, false),
  END_ADDRS_OFFSET(1, false),
  STARTLOOP_ADDRS_OFFSET(2, false),
  ENDLOOP_ADDRS_OFFSET(3, false),
  START_ADDRS_COARSE_OFFSET(4, false),
  MOD_LFO_TO_PITCH(5),
  VIB_LFO_TO_PITCH(6),
  MOD_ENV_TO_PITCH(7),
  INITIAL_FILTER_FC(8),
  INITIAL_FILTER_Q(9),
  MOD_LFO_TO_FILTER_FC(10),
  MOD_ENV_TO_FILTER_FC(11),
  END_ADDRS_COARSE_OFFSET(12, false),
  MOD_LFO_TO_VOLUME(13),
  CHORUS_EFFECTS_SEND(15),
  REVERB_EFFECTS_SEND(16),
  PAN(17),
  DELAY_MOD_LFO(21),
  FREQ_MOD_LFO(22),
  DELAY_VIB_LFO(23),
  FREQ_VIB_LFO(24),
  DELAY_MOD_ENV(25),
  ATTACK_MOD_ENV(26),
  HOLD_MOD_ENV(27),
  DECAY_MOD_ENV(28),
  SUSTAIN_MOD_ENV(29),
  RELEASE_MOD_ENV(30),
  KEYNUM_TO_MOD_ENV_HOLD(31),
  KEYNUM_TO_MOD_ENV_DECAY(32),
  DELAY_VOL_ENV(33),
  ATTACK_VOL_ENV(34),
  HOLD_VOL_ENV(35),
  DECAY_VOL_ENV(36),
  SUSTAIN_VOL_ENV(37),
  RELEASE_VOL_END(38),
  KEYNUM_TO_VOL_ENV_HOLD(39),
  KEYNUM_TO_VOL_ENV_DECAY(40),  
  INSTRUMENT(41, true, false),
  KEY_RANGE(43),
  VELOCITY_RANGE(44),
  STARTLOOP_ADDRS_COARSE_OFFSET(45, false),
  KEYNUM(46, false),
  VELOCITY(47, false),
  INITIAL_ATTENUATION(48),
  ENDLOOP_ADDRS_COARSE_OFFSET(50, false),
  COARSE_TUNE(51),
  FINE_TUNE(52),
  SAMPLE_ID(53, false),
  SAMPLE_MODES(54, false),
  SCALE_TUNING(56),
  EXCLUSIVE_CLASS(57, false),
  OVERRIDING_ROOT_KEY(58, false),
  END_OPER(60);
  
  /**
   * Creates a new enum value.
   * @param type - The enum value in the SoundFont format.
   * @param validForPreset - True if this operator would be valid in a preset zone.
   * @param validForInstrument - True if this operator would be valid in an instrument zone.
   */
  Operator(int type, boolean validForPreset, boolean validForInstrument) {
    type_ = type;
    validForPreset_ = validForPreset;
    validForInstrument_ = validForInstrument;
  }

  /**
   * Creates a new enum value that is valid for an instrument zone.
   * @param type - The enum value in the SoundFont format.
   * @param validForPreset - True if this operator would be valid in a preset zone.
   */
  Operator(int type, boolean validForPreset) {
    this(type, validForPreset, true);
  }

  /**
   * Creates a new enum value that is valid for a preset or instrument zone.
   * @param type - The enum value in the SoundFont format.
   */
  Operator(int type) {
    this(type, true);
  }

  /**
   * @return The SoundFont enum value for this operator.
   */
  public int getType() {
    return type_;
  }

  /**
   * @return True if this operator is valid in a preset zone.
   */
  public boolean isValidForPreset() {
    return validForPreset_;
  }

  /**
   * @return True if this operator is valid in an instrument zone.
   */
  public boolean isValidForInstrument() {
    return validForInstrument_;
  }

  /**
   * @return The operator corresponding to the given type.
   * @param type - The enum value in the SoundFont format.
   */
  public static Operator fromType(int type) {
    // TODO(klimt): Keep an index instead of linear search.
    for (Operator op : Operator.values()) {
      if (op.getType() == type) {
        return op;
      }
    }
    return UNSUPPORTED;
  }

  private final int type_;
  private final boolean validForPreset_;
  private final boolean validForInstrument_;
}
