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

package com.google.synthesizer.core.music;

import java.util.ArrayList;
import java.util.Collections;
import java.util.ListIterator;
import java.util.PriorityQueue;
import java.util.logging.Logger;

import com.google.synthesizer.core.model.composite.MultiChannelSynthesizer;
import com.google.synthesizer.core.music.Music.Event;
import com.google.synthesizer.core.music.Music.Score;

/**
 * Methods for playing a score using a synthesizer.
 * @see ScorePlayerListener
 */
public class ScorePlayer {
  /**
   * Creates a new ScorePlayer.
   */
  public ScorePlayer() {
    logger_ = Logger.getLogger(ScorePlayer.class.getName());
    playing_ = false;
  }

  /**
   * Merges identical loop events in a sorted list of events.  Since having two loops over the same
   * time period would normally make them multiplicative, and that isn't useful, this changes that
   * one situation to make them additive.  That way, you can have a segment that play three times
   * without having to create any events that have a repeat count other than one.
   */
  private void mergeIdenticalLoopEvents(ArrayList<Event.Builder> startList) {
    for (int i = 0; i < startList.size(); ++i) {
      if (i+1 < startList.size() &&
          startList.get(i).hasLoopEvent() &&
          startList.get(i+1).hasLoopEvent() &&
          startList.get(i).getStart() == startList.get(i+1).getStart() &&
          startList.get(i).getEnd() == startList.get(i+1).getEnd()) {
        startList.get(i).getLoopEventBuilder().setCount(
            startList.get(i).getLoopEventBuilder().getCount() +
            startList.get(i+1).getLoopEventBuilder().getCount());
        startList.remove(i+1);
        i--;
      }
    }
  }

  /**
   * Plays the given score using the given synthesizer.  Returns immediately, while the music plays
   * in a separate thread.
   * 
   * @param synth - the synthesizer to use for output.
   * @param score - the score to play.
   * @param beatsPerMinute - the tempo to play.
   * @param beatsPerMeasure - the time signature.
   * @param listener - the listener to notify of events.
   */
  public synchronized void startPlaying(MultiChannelSynthesizer synth,
                                        Score score,
                                        double beatsPerMinute,
                                        int beatsPerMeasure,
                                        ScorePlayerListener listener) {
    if (playing_) {
      logger_.warning("startPlaying called while already playing.");
      return;
    }

    // Copy the sequence so that it's safe to modify it while it's playing asynchronously.
    ArrayList<Event.Builder> startList =
        new ArrayList<Event.Builder>(Score.newBuilder(score).getEventBuilderList());
    Collections.sort(startList, EventComparator.byStart());
    mergeIdenticalLoopEvents(startList);
    starts_ = startList.listIterator();

    ends_ = new PriorityQueue<Event.Builder>(startList.size(), EventComparator.byEnd());

    // Setup the internal state.
    synth_ = synth;
    listener_ = listener;
    beatsPerMinute_ = beatsPerMinute;
    beatsPerMeasure_ = beatsPerMeasure;
    playing_ = true;
    stop_ = false;
    currentTime_ = 0.0;

    listener_.onStart();

    new Thread("ScorePlayer.startPlaying()") {
      public void run() {
        logger_.info("ScorePlayer.startPlaying() thread running.");
        while (true) {
          // While we have the lock, we'll need to see how long to sleep until the next iteration of
          // the loop.  This variable will contain that delay at the end of the loop.
          long delay;

          synchronized (ScorePlayer.this) {
            onTimeUpdate();

            // Check if it's time to stop.
            if (stop_) {
              playing_ = false;
              
              // Okay, now finish off all of the events that have been started.
              while (!ends_.isEmpty()) {
                Event.Builder event = ends_.remove();
                if (event.hasKeyEvent()) {
                  synth_.onNoteOff(event.getKeyEvent().getChannel(), event.getKey(), 0);
                }
              }
              
              listener_.onStop();
              logger_.info("Finished playing.");
              break;
            }

            // See if there are more events to process.
            if (!starts_.hasNext() && ends_.isEmpty()) {
              // There are no more events to process.
              stop_ = true;
              continue;
            }

            // There are more events.  See when the next one should be processed.
            boolean isEnd;
            double targetTime;
            if (!starts_.hasNext()) {
              // All of the start events are over.  The next event will need to be an end event.
              isEnd = true;
              targetTime = getSecondsPerMeasure() * ends_.peek().getEnd();
            } else if (ends_.isEmpty()) {
              // There are no end events queued up yet.  The next event will need to be a start.
              isEnd = false;
              targetTime = getSecondsPerMeasure() * starts_.next().getStart();
              starts_.previous();
            } else {
              // We'll need to process either a start or an end, whichever is first.
              if (ends_.peek().getEnd() <= peek(starts_).getStart()) {
                // Process the next end.
                isEnd = true;
                targetTime = getSecondsPerMeasure() * ends_.peek().getEnd();
              } else {
                // Process the next start.
                isEnd = false;
                targetTime = getSecondsPerMeasure() * peek(starts_).getStart();
              }
            }

            // See if we need to process an event now.
            if (currentTime_ >= targetTime) {
              // Yes, we do.
              if (isEnd) {
                Event.Builder event = ends_.remove();
                if (event.hasKeyEvent()) {
                  synth_.onNoteOff(event.getKeyEvent().getChannel(),
                                   event.getKey(), 0);
                } else if (event.hasLoopEvent()) {
                  if (!event.getLoopEvent().hasCountRemaining()) {
                    event.getLoopEventBuilder().setCountRemaining(event.getLoopEvent().getCount());
                  }
                  if (event.getLoopEvent().getCountRemaining() > 0) {
                    // Okay, it's time to loop.
                    // TODO(klimt):  Ideally, we'd kill all note events currently alive, but we
                    // don't want to kill any of the loop events, so it's a little tricky.
                    // while (!ends_.isEmpty()) {
                    //   Event.Builder eventToStop = ends_.remove();
                    //   if (eventToStop.hasKeyEvent()) {
                    //     synth_.onNoteOff(eventToStop.getKeyEvent().getChannel(),
                    //                      eventToStop.getKey(), 0);
                    //   }
                    // }
                    // Now, rewind the start list until it's before the start time.
                    while (starts_.previous() != event) {
                      // Rewind.
                    }
                    // Okay, now update the current time.
                    currentTime_ = getSecondsPerMeasure() * event.getStart();
                    targetTime = currentTime_;
                    event.getLoopEventBuilder().setCountRemaining(
                        event.getLoopEvent().getCountRemaining() - 1);
                  } else {
                    // We're finished with this loop, so we're moving on.
                    event.getLoopEventBuilder().clearCountRemaining();
                  }
                }
              } else {
                Event.Builder event = starts_.next();
                if (event.hasKeyEvent()) {
                  synth_.onNoteOn(event.getKeyEvent().getChannel(), event.getKey(), 127);
                }
                // We started the event, so let's make sure we end it eventually. :)
                ends_.add(event);
              }
              // Okay, move on to the next event.
              continue;
            }

            // If we got this far, we'll need to sleep until the next event.  See how long.
            delay = (long)((targetTime - currentTime_) * 1000.0);            
          }  // synchronized (ScorePlayer.this)

          // Sleep for a while.
          if (delay > 250) {
            delay = 250;
          }
          try {
            Thread.sleep(delay);
          } catch (InterruptedException e) {
            logger_.warning("Sequence.play() thread interrupted.");
          }

          synchronized (ScorePlayer.this) {
            currentTime_ += (delay / 1000.0);
          }
        }  // while (true)
      }  // run()
    }.start();
  }

  /**
   * This is a stupid hack because ListIterator doesn't have a peek method.
   */
  private static Event.Builder peek(ListIterator<Event.Builder> iterator) {
    Event.Builder event = iterator.next();
    iterator.previous();
    return event;
  }
  
  /**
   * Stops playback as soon as convenient.  Doesn't block.
   */
  public synchronized void stopPlaying() {
    stop_ = true;
  }

  /**
   * Returns the number of seconds each measure of the song should take.
   */
  private synchronized double getSecondsPerMeasure() {
    return (beatsPerMeasure_ / beatsPerMinute_) * 60.0;
  }

  /**
   * Calls the onTimeUpdate() method of the listener.
   */
  private synchronized void onTimeUpdate() {
    listener_.onTimeUpdate(currentTime_ / getSecondsPerMeasure());
  }

  // The synthesizer to use for playing.
  private MultiChannelSynthesizer synth_;

  // The tempo of the song.
  private double beatsPerMinute_;

  // The time signature of the score.
  private int beatsPerMeasure_;

  // The listener to notify of events.
  private ScorePlayerListener listener_;

  // Is the thread playing?
  private boolean playing_;

  // Should the thread stop?
  private boolean stop_;

  // The time of the current head of the playback, in seconds.
  private double currentTime_;

  // The sequence being played.
  private ListIterator<Event.Builder> starts_;
  private PriorityQueue<Event.Builder> ends_;

  private Logger logger_;
}
