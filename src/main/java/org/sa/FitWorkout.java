package org.sa;

import java.time.Instant;
import java.util.List;

public class FitWorkout {

  public final Instant startTime;
  public final Instant endTime;
  public final String sportName;
  public final List<HeartRateSample> samples;
  public final List<WorkoutEvent> events;

  public FitWorkout(Instant startTime, Instant endTime, String sportName,
                    List<HeartRateSample> samples, List<WorkoutEvent> events) {
    this.startTime = startTime;
    this.endTime = endTime;
    this.sportName = sportName;
    this.samples = samples;
    this.events = events;
  }

  public static class HeartRateSample {
    public final Instant timestamp;
    public final Integer heartRate; // null if not present on this record

    public HeartRateSample(Instant timestamp, Integer heartRate) {
      this.timestamp = timestamp;
      this.heartRate = heartRate;
    }
  }

  public static class WorkoutEvent {
    public final Instant timestamp;
    public final EventType type;

    public WorkoutEvent(Instant timestamp, EventType type) {
      this.timestamp = timestamp;
      this.type = type;
    }
  }

  public enum EventType {
    PAUSED, RESUMED, OTHER
  }
}