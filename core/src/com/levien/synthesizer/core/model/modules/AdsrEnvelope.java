/*
 * Copyright 2010 Google Inc.
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

package com.levien.synthesizer.core.model.modules;

import com.levien.synthesizer.core.model.CachedSignalProvider;
import com.levien.synthesizer.core.model.Envelope;
import com.levien.synthesizer.core.model.SignalProvider;
import com.levien.synthesizer.core.model.SynthesisTime;

/**
 * An ADSR is the most common envelope for a synth.
 */
public class AdsrEnvelope extends CachedSignalProvider implements Envelope {
  /**
   * Creates an ADSR with the given parameters.
   * @param attack - attack time in seconds.
   * @param decay - decay time in seconds.
   * @param sustain - sustain level from 0 to 1.
   * @param release - release time in seconds.
   */
  public AdsrEnvelope(SignalProvider attack,
                      SignalProvider decay,
                      SignalProvider sustain,
                      SignalProvider release) {
    value_ = 0.0;
    attacking_ = false;

    trigger_ = false;
    gate_ = false;

    attack_ = attack;
    decay_ = decay;
    sustain_ = sustain;
    release_ = release;
  }

  public synchronized double computeValue(SynthesisTime time) {
    if (trigger_) {
      attacking_ = true;
      trigger_ = false;
      // It doesn't really matter if the attack is on the leading or trailing edge of the trigger.
      // By having it start on the trailing edge for 0 and the leading edge otherwise, it makes the
      // math a tiny bit simpler and makes this a little easier to unit test.
      if (value_ == 0) {
        return value_;
      }
    }

    if (gate_) {
      double sustain = sustain_.getValue(time);
      if (attacking_) {
        // Attacking.
        double attack = attack_.getValue(time);
        if (attack > 0.0) {
          double timeDelta = time.getDeltaTime();
          value_ += timeDelta / attack;
          if (value_ >= 1.0) {
            // TODO(klimt): This isn't exactly right.  We should really figure out what time it
            // would've reached zero, and go ahead and apply the decay for that extra time that
            // has passed.
            value_ = 1.0;
            attacking_ = false;
          }
        } else {
          value_ = 1.0;
          attacking_ = false;
        }
      } else if (value_ > sustain) {
        // Decaying.
        double decay = decay_.getValue(time);
        if (decay > 0.0) {
          double timeDelta = time.getDeltaTime();
          value_ += timeDelta * ((sustain - 1.0) / decay);
          if (value_ < sustain) {
            value_ = sustain;
          }
        } else {
          value_ = sustain;
        }
      } else if (value_ < sustain) {
        // Decaying backwards.
        // This shouldn't _normally_ happen, but can if you change the parameters over time.
        double decay = decay_.getValue(time);
        if (decay > 0.0) {
          double timeDelta = time.getDeltaTime();
          value_ -= timeDelta * ((sustain - 1.0) / decay);
          if (value_ > sustain) {
            value_ = sustain;
          }
        } else {
          value_ = sustain;
        }
      }
    } else {
      // Releasing.
      if (value_ > 0.0) {
        double release = release_.getValue(time);
        if (release > 0.0) {
          double sustain = sustain_.getValue(time);
          if (sustain == 0.0) {
            double decay = decay_.getValue(time);
            if (decay == 0.0) {
              value_ = 0.0;
            } else {
              double timeDelta = time.getDeltaTime();
              value_ -= timeDelta * ((sustain - 1.0) / decay);
            }
          } else {
            double timeDelta = time.getDeltaTime();
            value_ -= timeDelta * (sustain / release);
          }
          if (value_ < 0.0) {
            value_ = 0.0;
          }
        } else {
          value_ = 0.0;
        }
      }
    }
    return value_; 
  }

  public synchronized void turnOn(boolean retriggerIfOn) {
    if (gate_ && !retriggerIfOn) {
      return;
    }
    trigger_ = true;
    gate_ = true;
  }
  
  public synchronized void turnOff() {
    gate_ = false;
  }

  /**
   * Returns whether the envelope is currently being triggered.
   */
  public synchronized boolean getTrigger() {
    return trigger_;
  }
  
  /**
   * Returns whether the key for this envelope is currently being held down.
   */
  public synchronized boolean getGate() {
    return gate_;
  }

  // The most recently output value from this envelope.
  private double value_;

  // Are we in the attack phase?
  private boolean attacking_;

  // Whether the envelope is "on".
  private boolean trigger_;
  private boolean gate_;

  // The envelope shape parameters.
  private SignalProvider attack_;
  private SignalProvider decay_;
  private SignalProvider sustain_;
  private SignalProvider release_;
}
