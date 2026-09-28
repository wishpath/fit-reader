package org.sa;

import com.garmin.fit.Decode;
import com.garmin.fit.Field;
import com.garmin.fit.Mesg;
import com.garmin.fit.MesgBroadcaster;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class FitParseUtil {

  // FIT epoch (1989-12-31T00:00:00Z) offset from Unix epoch, in seconds
  private static final long FIT_EPOCH_OFFSET = 631065600L;

  public static FitWorkout parse(byte[] fitData) throws Exception {
    Decode decode = new Decode();
    MesgBroadcaster broadcaster = new MesgBroadcaster(decode);

    long[] fileTimeCreated = {-1};
    long[] sportCode = {-1};
    List<FitWorkout.HeartRateSample> samples = new ArrayList<>();
    List<FitWorkout.WorkoutEvent> events = new ArrayList<>();

    broadcaster.addListener((com.garmin.fit.MesgListener) mesg -> {
      switch (mesg.getName()) {
        case "file_id":
          fileTimeCreated[0] = longValue(mesg, "time_created", fileTimeCreated[0]);
          break;

        case "session":
          sportCode[0] = longValue(mesg, "sport", sportCode[0]);
          fileTimeCreated[0] = longValue(mesg, "start_time", fileTimeCreated[0]);
          break;

        case "event":
          long eventTs = longValue(mesg, "timestamp", -1);
          if (eventTs != -1) {
            long eventType = longValue(mesg, "event_type", -1);
            events.add(new FitWorkout.WorkoutEvent(toInstant(eventTs), toEventType(eventType)));
          }
          break;

        case "record":
          long recordTs = longValue(mesg, "timestamp", -1);
          if (recordTs != -1) {
            long hr = longValue(mesg, "heart_rate", -1);
            Integer hrValue = hr != -1 ? (int) hr : null;
            samples.add(new FitWorkout.HeartRateSample(toInstant(recordTs), hrValue));
          }
          break;

        default:
          // ignore other message types
      }
    });

    try (InputStream in = new ByteArrayInputStream(fitData)) {
      broadcaster.run(in);
    }

    List<FitWorkout.HeartRateSample> dedupedSamples = mergeSamplesWithSameTimestamp(samples);
    List<FitWorkout.WorkoutEvent> cleanedEvents = removeStartupEvent(events, dedupedSamples);

    Instant startTime = fileTimeCreated[0] != -1 ? toInstant(fileTimeCreated[0]) : null;
    Instant endTime = dedupedSamples.isEmpty() ? null : dedupedSamples.get(dedupedSamples.size() - 1).timestamp;
    String sportName = sportName(sportCode[0]);

    return new FitWorkout(startTime, endTime, sportName, dedupedSamples, cleanedEvents);
  }

  private static FitWorkout.EventType toEventType(long eventType) {
    if (eventType == 0) {
      return FitWorkout.EventType.RESUMED;
    } else if (eventType == 1) {
      return FitWorkout.EventType.PAUSED;
    } else {
      return FitWorkout.EventType.OTHER;
    }
  }

  /** Merges consecutive records sharing the same timestamp, keeping the HR value if either has one. */
  private static List<FitWorkout.HeartRateSample> mergeSamplesWithSameTimestamp(List<FitWorkout.HeartRateSample> samples) {
    List<FitWorkout.HeartRateSample> merged = new ArrayList<>();
    for (FitWorkout.HeartRateSample sample : samples) {
      if (!merged.isEmpty() && merged.get(merged.size() - 1).timestamp.equals(sample.timestamp)) {
        FitWorkout.HeartRateSample previous = merged.remove(merged.size() - 1);
        Integer heartRate = sample.heartRate != null ? sample.heartRate : previous.heartRate;
        merged.add(new FitWorkout.HeartRateSample(sample.timestamp, heartRate));
      } else {
        merged.add(sample);
      }
    }
    return merged;
  }

  /** Drops any event at/before the first sample — that's the implicit workout start, not a real pause/resume. */
  private static List<FitWorkout.WorkoutEvent> removeStartupEvent(List<FitWorkout.WorkoutEvent> events, List<FitWorkout.HeartRateSample> samples) {
    if (samples.isEmpty()) {
      return events;
    }
    Instant firstSampleTime = samples.get(0).timestamp;
    List<FitWorkout.WorkoutEvent> cleaned = new ArrayList<>(events);
    cleaned.removeIf(e -> !e.timestamp.isAfter(firstSampleTime));
    return cleaned;
  }

  private static long longValue(Mesg mesg, String fieldName, long fallback) {
    Field field = mesg.getField(fieldName);
    if (field == null || field.getNumValues() == 0) {
      return fallback;
    }
    Object value = field.getValue(0);
    return value instanceof Number ? ((Number) value).longValue() : fallback;
  }

  private static Instant toInstant(long fitTimestamp) {
    return Instant.ofEpochSecond(fitTimestamp + FIT_EPOCH_OFFSET);
  }

  private static String sportName(long sport) {
    switch ((int) sport) {
      case 0: return "Generic";
      case 1: return "Running";
      case 2: return "Cycling";
      case 4: return "Fitness Equipment";
      case 5: return "Swimming";
      case 10: return "Training / CrossFit";
      case 11: return "Walking";
      case 15: return "Rowing";
      case 17: return "Hiking";
      case 18: return "Multisport";
      default: return "Sport #" + sport;
    }
  }
}