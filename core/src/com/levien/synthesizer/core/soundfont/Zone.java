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

package com.levien.synthesizer.core.soundfont;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * A zone is like a flattened bag that contains any of the settings that could be in a bag.
 */
@SuppressWarnings("unused")
public class Zone {
  /**
   * Creates a new Zone that's empty.
   */
  public Zone() {
    logger_ = Logger.getLogger(getClass().getName());
    modulatorList_ = new ArrayList<Modulator>();
  }

  /**
   * Copies all of the values from another zone into this one.
   */
  public void CopyFrom(Zone other) {
    instrument_ = other.instrument_;
    sample_ = other.sample_;
    startAddrsOffset_ = other.startAddrsOffset_;
    endAddrsOffset_ = other.endAddrsOffset_;
    startloopAddrsOffset_ = other.startloopAddrsOffset_;
    endloopAddrsOffset_ = other.endloopAddrsOffset_;
    startAddrsCoarseOffset_ = other.startAddrsCoarseOffset_;
    endAddrsCoarseOffset_ = other.endAddrsCoarseOffset_;
    startloopAddrsCoarseOffset_ = other.startloopAddrsCoarseOffset_;
    endloopAddrsCoarseOffset_ = other.endloopAddrsCoarseOffset_;
    modLfoToPitch_ = other.modLfoToPitch_;
    vibLfoToPitch_ = other.vibLfoToPitch_;
    modEnvToPitch_ = other.modEnvToPitch_;
    initialFilterFc_ = other.initialFilterFc_;
    initialFilterQ_ = other.initialFilterQ_;
    modLfoToFilterFc_ = other.modLfoToFilterFc_;
    modEnvToFilterFc_ = other.modEnvToFilterFc_;
    modLfoToVolume_ = other.modLfoToVolume_;
    chorusEffectsSend_ = other.chorusEffectsSend_;
    reverbEffectsSend_ = other.reverbEffectsSend_;
    pan_ = other.pan_;
    delayModLFO_ = other.delayModLFO_;
    freqModLFO_ = other.freqModLFO_;
    delayVibLFO_ = other.delayVibLFO_;
    freqVibLFO_ = other.freqVibLFO_;
    delayModEnv_ = other.delayModEnv_;
    attackModEnv_ = other.attackModEnv_;
    holdModEnv_ = other.holdModEnv_;
    decayModEnv_ = other.decayModEnv_;
    sustainModEnv_ = other.sustainModEnv_;
    releaseModEnv_ = other.releaseModEnv_;
    keynumToModEnvHold_ = other.keynumToModEnvHold_;
    keynumToModEnvDecay_ = other.keynumToModEnvDecay_;
    delayVolEnv_ = other.delayVolEnv_;
    attackVolEnv_ = other.attackVolEnv_;
    holdVolEnv_ = other.holdVolEnv_;
    decayVolEnv_ = other.decayVolEnv_;
    sustainVolEnv_ = other.sustainVolEnv_;
    releaseVolEnv_ = other.releaseVolEnv_;
    keynumToVolEnvHold_ = other.keynumToVolEnvHold_;
    keynumToVolEnvDecay_ = other.keynumToVolEnvDecay_;
    minKey_ = other.minKey_;
    maxKey_ = other.maxKey_;
    minVelocity_ = other.minVelocity_;
    maxVelocity_ = other.maxVelocity_;
    keynum_ = other.keynum_;
    velocity_ = other.velocity_;
    initialAttenuation_ = other.initialAttenuation_;
    coarseTune_ = other.coarseTune_;
    fineTune_ = other.fineTune_;
    sampleMode_ = other.sampleMode_;
    scaleTuning_ = other.scaleTuning_;
    exclusiveClass_ = other.exclusiveClass_;
    overridingRootKey_ = other.overridingRootKey_;
    modulatorList_ = new ArrayList<Modulator>(other.modulatorList_);
  }

  /**
   * @return A new Zone equivalent to this one.
   */
  public Zone copy() {
    Zone other = new Zone();
    other.CopyFrom(this);
    return other;
  }

  /**
   * @return The input converted from cents to octaves.
   */
  private static double convertCentsToOctaves(double value) {
    return value / 1200.0;
  }

  /**
   * @return The input converted from cents to seconds.
   */
  private static double convertCentsToSeconds(double value) {
    return Math.pow(2.0, convertCentsToOctaves(value));
  }

  /**
   * Adds a preset generator to this Zone.
   * @param generator - The generator to add.
   * @param instruments - The list of all instruments in this SoundFont file.
   */
  public void addPresetGenerator(Generator generator,
                                 List<Instrument> instruments) {
    if (!generator.getOperator().isValidForPreset()) {
      throw new IllegalArgumentException("Invalid operator for preset: " +
                                         generator.getOperator() + ".");
    }
    addGenerator(generator, instruments, null);
  }
  
  /**
   * Adds an instrument generator to this Zone.
   * @param generator - The generator to add.
   * @param samples - The list of all samples in this SoundFont file.
   */
  public void addInstrumentGenerator(Generator generator,
                                     List<Sample> samples) {
    if (!generator.getOperator().isValidForInstrument()) {
      throw new IllegalArgumentException("Invalid operator for instrument: " + 
                                         generator.getOperator() + ".");
    }
    addGenerator(generator, null, samples);
  }

  /**
   * Internal method to add a generator of either type.
   */
  protected void addGenerator(Generator generator,
                              List<Instrument> instruments,
                              List<Sample> samples) {
    short amount = generator.getAmount();
    //
    // This ridiculous switch statement takes the given operator and decodes its argument into the
    // right values in the Zone.
    //
    switch (generator.getOperator()) {
      case UNSUPPORTED: {
        break;
      }
      case START_ADDRS_OFFSET: {
        startAddrsOffset_ = amount; break;
      }
      case END_ADDRS_OFFSET: {
        endAddrsOffset_ = amount; break;
      }
      case STARTLOOP_ADDRS_OFFSET: {
        startloopAddrsOffset_ = amount; break;
      }
      case ENDLOOP_ADDRS_OFFSET: {
        endloopAddrsOffset_ = amount; break;
      }
      case START_ADDRS_COARSE_OFFSET: {
        startAddrsCoarseOffset_ = amount * 32768; break;
      }
      case MOD_LFO_TO_PITCH: {
        modLfoToPitch_ = convertCentsToOctaves(amount); break;
      }
      case VIB_LFO_TO_PITCH: {
        vibLfoToPitch_ = convertCentsToOctaves(amount); break;
      }
      case MOD_ENV_TO_PITCH: {
        modEnvToPitch_ = convertCentsToOctaves(amount); break;
      }
      case INITIAL_FILTER_FC: {
        initialFilterFc_ = convertCentsToOctaves(amount); break;
      }
      case INITIAL_FILTER_Q: {
        initialFilterQ_ = amount; break;
      }
      case MOD_LFO_TO_FILTER_FC: {
        modLfoToFilterFc_ = convertCentsToOctaves(amount); break;
      }
      case MOD_ENV_TO_FILTER_FC: {
        modEnvToFilterFc_ = convertCentsToOctaves(amount); break;
      }
      case END_ADDRS_COARSE_OFFSET: {
        endAddrsCoarseOffset_ = amount * 32768; break;
      }
      case MOD_LFO_TO_VOLUME: {
        modLfoToVolume_ = amount; break;
      }
      case CHORUS_EFFECTS_SEND: {
        chorusEffectsSend_ = amount / 1000.0; break;
      }
      case REVERB_EFFECTS_SEND: {
        reverbEffectsSend_ = amount / 1000.0; break;
      }
      case PAN: {
        pan_ = amount / 1000.0; break;
      }
      case DELAY_MOD_LFO: {
        delayModLFO_ = convertCentsToSeconds(amount); break;
      }
      case FREQ_MOD_LFO: {
        freqModLFO_ = convertCentsToOctaves(amount); break;
      }
      case DELAY_VIB_LFO: {
        delayVibLFO_ = convertCentsToSeconds(amount); break;
      }
      case FREQ_VIB_LFO: {
        freqVibLFO_ = convertCentsToOctaves(amount); break;
      }
      case DELAY_MOD_ENV: {
        delayModEnv_ = convertCentsToSeconds(amount); break;
      }
      case ATTACK_MOD_ENV: {
        attackModEnv_ = convertCentsToSeconds(amount); break;
      }
      case HOLD_MOD_ENV: {
        holdModEnv_ = convertCentsToSeconds(amount); break;
      }
      case DECAY_MOD_ENV: {
        decayModEnv_ = convertCentsToSeconds(amount); break;
      }
      case SUSTAIN_MOD_ENV: {
        sustainModEnv_ = 1.0 - (amount / -1000.0); break;
      }
      case RELEASE_MOD_ENV: {
        releaseModEnv_ = convertCentsToSeconds(amount); break;
      }
      case KEYNUM_TO_MOD_ENV_HOLD: {
        keynumToModEnvHold_ = convertCentsToSeconds(amount); break;
      }
      case KEYNUM_TO_MOD_ENV_DECAY: {
        keynumToModEnvDecay_ = convertCentsToSeconds(amount); break;
      }
      case DELAY_VOL_ENV: {
        delayVolEnv_ = convertCentsToSeconds(amount); break;
      }
      case ATTACK_VOL_ENV: {
        attackVolEnv_ = convertCentsToSeconds(amount); break;
      }
      case HOLD_VOL_ENV: {
        holdVolEnv_ = convertCentsToSeconds(amount); break;
      }
      case DECAY_VOL_ENV: {
        decayVolEnv_ = convertCentsToSeconds(amount); break;
      }
      case SUSTAIN_VOL_ENV: {
        sustainVolEnv_ = 1.0 - (amount / -1000.0); break;
      }
      case RELEASE_VOL_END: {
        releaseVolEnv_ = convertCentsToSeconds(amount); break;
      }
      case KEYNUM_TO_VOL_ENV_HOLD: {
        keynumToVolEnvHold_ = convertCentsToSeconds(amount); break;
      }
      case KEYNUM_TO_VOL_ENV_DECAY: {
        keynumToVolEnvDecay_ = convertCentsToSeconds(amount); break;
      }  
      case INSTRUMENT: {
        instrument_ = instruments.get(((int)amount) & 0xFFFF); break;
      }
      case KEY_RANGE: {
        minKey_ = amount & 0xFF;
        maxKey_ = (amount >> 8) & 0xFF;
        break;
      }
      case VELOCITY_RANGE: {
        minVelocity_ = amount & 0xFF;
        maxVelocity_ = (amount >> 8) & 0xFF;
        break;
      }
      case STARTLOOP_ADDRS_COARSE_OFFSET: {
        startloopAddrsCoarseOffset_ = amount * 32768; break;
      }
      case KEYNUM: {
        keynum_ = amount & 0xFF; break;
      }
      case VELOCITY: {
        velocity_ = amount & 0xFF; break;
      }
      case INITIAL_ATTENUATION: {
        initialAttenuation_ = amount; break;
      }
      case ENDLOOP_ADDRS_COARSE_OFFSET: {
        endloopAddrsCoarseOffset_ = amount * 32768; break;
      }
      case COARSE_TUNE: {
        coarseTune_ = amount / 12.0; break;
      }
      case FINE_TUNE: {
        fineTune_ = convertCentsToOctaves(amount); break;
      }
      case SAMPLE_ID: {
        sample_ = samples.get(((int)amount) & 0xFFFF); break;
      }
      case SAMPLE_MODES: {
        switch (amount & 0x03) {
          case 0: sampleMode_ = SampleMode.NO_LOOP; break;
          case 1: sampleMode_ = SampleMode.LOOP_CONTINUOUSLY; break;
          case 2: sampleMode_ = SampleMode.NO_LOOP; break;
          case 3: sampleMode_ = SampleMode.LOOP_CONTINUOUSLY_THEN_FINISH; break;
        }
        break;
      }
      case SCALE_TUNING: {
        scaleTuning_ = convertCentsToOctaves(amount); break;
      }
      case EXCLUSIVE_CLASS: {
        exclusiveClass_ = amount & 0xFF; break;
      }
      case OVERRIDING_ROOT_KEY: {
        overridingRootKey_ = amount & 0xFF; break;
      }
      case END_OPER: {
        break;
      }
    }
  }

  /**
   * Adds a modulator the zone.
   */
  public void addModulator(Modulator modulator) {
    modulatorList_.add(modulator);
  }

  /**
   * @return the index of the first sample in this instrument zone.
   * Adjusts based on the generator settings.
   */
  public long getStart() {
    long start = startAddrsOffset_ + startAddrsCoarseOffset_;
    if (sample_ != null) {
      start += sample_.getStart();
    }
    return start;
  }

  /**
   * @return the index one after the last sample in this instrument zone.
   * Adjusts based on the generator settings.
   */
  public long getEnd() {
    long end = endAddrsOffset_ + endAddrsCoarseOffset_;
    if (sample_ != null) {
      end += sample_.getEnd();
    }
    return end;
  }

  /**
   * @return the number of samples in this instrument zone.
   * Adjusts based on the generator settings.
   */
  public long getCount() {
    return getEnd() - getStart();
  }

  /**
   * @return the index of the first sample in this instrument zone's loop subset.
   * Adjusts based on the generator settings.
   */
  public long getStartLoop() {
    long startloop = startloopAddrsOffset_ + startloopAddrsCoarseOffset_;
    if (sample_ != null) {
      startloop += sample_.getStart();
    }
    return startloop;
  }

  /**
   * @return the index one after the last sample in this instrument zone's loop subset.
   * Adjusts based on the generator settings.
   */
  public long getEndLoop() {
    long endloop = endloopAddrsOffset_ + endloopAddrsCoarseOffset_;
    if (sample_ != null) {
      endloop += sample_.getEnd();
    }
    return endloop;
  }

  /**
   * @return The instrument associated with this preset zone.
   */
  public Instrument getInstrument() {
    return instrument_;
  }

  /**
   * @return True if this instrument zone can handle the given key.
   */
  public boolean inKeyRange(int key) {
    return ((minKey_ < 0 || key >= minKey_) &&
            (maxKey_ < 0 || key <= maxKey_));
  }

  /**
   * @return True if this instrument zone can handle the given velocity.
   */
  public boolean inVelocityRange(int velocity) {
    return ((minVelocity_ < 0 || velocity >= minVelocity_) &&
            (maxVelocity_ < 0 || velocity <= maxVelocity_));
  }

  /**
   * @return The sample associated with this instrument zone.
   */
  public Sample getSample() {
    return sample_;
  }

  /**
   * @return The sample mode for this instrument zone.
   */
  public SampleMode getSampleMode() {
    return sampleMode_;
  }

  private Logger logger_;

  //
  // The various settings for a zone, according to the SoundFont 2.1 format specification.
  // Most of these are ignored in this synthesizer implementation, but some are not.
  //

  private Instrument instrument_ = null;  // Set for a preset zone.
  private Sample sample_ = null;  // Set for an instrument zone.
  
  // All in samples.
  private int startAddrsOffset_ = 0;
  private int endAddrsOffset_ = 0;
  private int startloopAddrsOffset_ = 0;
  private int endloopAddrsOffset_ = 0;
  private int startAddrsCoarseOffset_ = 0;
  private int endAddrsCoarseOffset_ = 0;
  private int startloopAddrsCoarseOffset_ = 0;
  private int endloopAddrsCoarseOffset_ = 0;

  // All in log Hz.
  private double modLfoToPitch_ = 0;
  private double vibLfoToPitch_ = 0;
  private double modEnvToPitch_ = 0;

  private double initialFilterFc_ = convertCentsToOctaves(13500);  // in log Hz.
  private int initialFilterQ_ = 0;  // in centibels.
  private double modLfoToFilterFc_ = 0;  // in log Hz.
  private double modEnvToFilterFc_ = 0;  // in log Hz.

  private int modLfoToVolume_ = 0;  // in centibels.

  // All are percentages.
  private double chorusEffectsSend_ = 0.0;
  private double reverbEffectsSend_ = 0.0;
  private double pan_ = 0.0;

  private double delayModLFO_ = convertCentsToSeconds(-12000);  // in seconds.
  private double freqModLFO_ = 0;  // in log Hz.
  private double delayVibLFO_ = convertCentsToSeconds(-12000);  // in seconds.
  private double freqVibLFO_ = 0;  // in log Hz.

  // All in seconds, except sustain.
  private double delayModEnv_ = convertCentsToSeconds(-12000);
  private double attackModEnv_ = convertCentsToSeconds(-12000);
  private double holdModEnv_ = convertCentsToSeconds(-12000);
  private double decayModEnv_ = convertCentsToSeconds(-12000);
  private double sustainModEnv_ = 0;  // percentage.
  private double releaseModEnv_ = convertCentsToSeconds(-12000);

  // All in seconds per key.
  private double keynumToModEnvHold_ = 0;
  private double keynumToModEnvDecay_ = 0;

  // All in seconds, except sustain.
  private double delayVolEnv_ = convertCentsToSeconds(-12000);
  private double attackVolEnv_ = convertCentsToSeconds(-12000);
  private double holdVolEnv_ = convertCentsToSeconds(-12000);
  private double decayVolEnv_ = convertCentsToSeconds(-12000);
  private double sustainVolEnv_ = 0;  // percentage.
  private double releaseVolEnv_ = convertCentsToSeconds(-12000);

  // All in seconds per key.
  private double keynumToVolEnvHold_ = 0;
  private double keynumToVolEnvDecay_ = 0;

  // These are midi values in the range [0, 127].  -1 means unset.
  private int minKey_ = -1;
  private int maxKey_ = -1;
  private int minVelocity_ = -1;
  private int maxVelocity_ = -1;

  // These values override the input value.
  private int keynum_ = -1;
  private int velocity_ = -1;

  private int initialAttenuation_ = 0;  // in centibels.

  // All in log Hz.
  private double coarseTune_ = 0;
  private double fineTune_ = 0;

  enum SampleMode {
    NO_LOOP,
    LOOP_CONTINUOUSLY,
    LOOP_CONTINUOUSLY_THEN_FINISH,
  }
  private SampleMode sampleMode_ = SampleMode.NO_LOOP;

  private double scaleTuning_ = 0;  // in log Hz per key.
  private int exclusiveClass_ = 0;  // An id in range [1, 127], or 0 if not set.
  private int overridingRootKey_ = -1;  // A key in range [0, 127], or -1 if not set.

  // All of the modulators in this zone.  This is ignored.
  private ArrayList<Modulator> modulatorList_;
}
