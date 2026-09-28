package org.sa;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class AnalysisUtil {
  /** Minimum amount the window start is always pulled back, to account for HR sensor lag. */
  private static final int MINIMUM_HEART_RATE_LAG_SECONDS = 10;

  /** Hard cap on how far back the window start can be pulled, even if HR was still climbing. */
  private static final int MAXIMUM_HEART_RATE_LAG_SECONDS = 45;

  /**
   * Finds and prints the section of the given duration with the highest average heart rate,
   * followed by every record within that window.
   *
   * @param workout the parsed workout
   * @param durationText duration text, e.g. "30:00", "00:30:00", or "45" (seconds)
   * @throws IllegalArgumentException if duration is longer than the workout itself
   */
  public static void printHottestHeartRateWindow(FitWorkout workout, String durationText) {
    Duration duration = TimeUtil.parseDuration(durationText);
    HeartRateWindow window = findHighestAverageHeartRateWindow(workout, duration);
    Instant startWithHeartRateClimbed = window.start;
    System.out.println("initial start: " + startWithHeartRateClimbed);
    Instant startAdjustedForHeartRateLag = adjustStartForHeartRateLag(workout, window.start);
    System.out.println("adjusted start: " + startAdjustedForHeartRateLag + "\n");
    HeartRateWindow windowAdjustedForHeartRateLag = getHeartRateWindow(startAdjustedForHeartRateLag, duration, workout);

    ConsolePrintUtil.printHottestWindowReport(windowAdjustedForHeartRateLag, duration, workout);


    List<FitWorkout.HeartRateSample> heartRateSamplesBeforeWindow = workout.samples.stream()
        .filter(s -> !s.timestamp.isBefore(windowAdjustedForHeartRateLag.start.minusSeconds(20)) && !s.timestamp.isAfter(windowAdjustedForHeartRateLag.start.minusSeconds(1)))
        .collect(Collectors.toList());
    System.out.println("before window:");
    ConsolePrintUtil.printHeartRateSamples(heartRateSamplesBeforeWindow, workout);

    List<FitWorkout.HeartRateSample> heartRateSamplesInWindow = workout.samples.stream()
        .filter(s -> !s.timestamp.isBefore(windowAdjustedForHeartRateLag.start) && !s.timestamp.isAfter(windowAdjustedForHeartRateLag.end))
        .collect(Collectors.toList());

    System.out.println("\nDuring window window:");
    ConsolePrintUtil.printHeartRateSamples(heartRateSamplesInWindow, workout);
  }

  private static HeartRateWindow findHighestAverageHeartRateWindow(FitWorkout workout, Duration duration) {
    List<FitWorkout.HeartRateSample> samples = workout.samples.stream()
        .filter(s -> s.heartRate != null)
        .collect(Collectors.toList());

    if (samples.isEmpty()) {
      throw new IllegalStateException("Workout has no heart rate data");
    }

    Instant workoutStart = samples.get(0).timestamp;
    Instant workoutEnd = samples.get(samples.size() - 1).timestamp;
    Duration workoutDuration = Duration.between(workoutStart, workoutEnd);

    if (duration.compareTo(workoutDuration) > 0) {
      throw new IllegalArgumentException("Requested duration (" + format(duration)
          + ") is longer than the workout itself (" + format(workoutDuration) + ")");
    }

    // Two-pointer sliding window: for each right edge, shrink from the left
    // until the window fits within `duration`. Each sample enters/leaves the
    // running sum at most once => O(n) overall.
    int left = 0;
    long sum = 0;
    HeartRateWindow best = null;

    for (int right = 0; right < samples.size(); right++) {
      sum += samples.get(right).heartRate;

      while (Duration.between(samples.get(left).timestamp, samples.get(right).timestamp).compareTo(duration) > 0) {
        sum -= samples.get(left).heartRate;
        left++;
      }

      int count = right - left + 1;
      double average = (double) sum / count;

      if (best == null || average > best.averageHeartRate) {
        best = new HeartRateWindow(samples.get(left).timestamp, samples.get(right).timestamp, average, count);
      }
    }

    return best;
  }

  /**
   * Pulls the window start backwards to account for heart rate sensor lag: HR readings trail
   * the actual physical effort by several seconds, so the moment HR *starts climbing* toward
   * the hot window usually precedes the window's statistical start.
   * <p>
   * Always steps back at least {@link #MINIMUM_HEART_RATE_LAG_SECONDS}. From there, it keeps
   * stepping further back for as long as heart rate was non-decreasing walking forward through
   * time (i.e. it was on a climb into the window), up to a hard cap of
   * {@link #MAXIMUM_HEART_RATE_LAG_SECONDS}.
   */
  private static Instant adjustStartForHeartRateLag(FitWorkout workout, Instant windowStart) {
    List<FitWorkout.HeartRateSample> samples = workout.samples.stream().filter(s -> s.heartRate != null).collect(Collectors.toList());
    int startIndex = indexOfTimestamp(samples, windowStart);
    if (startIndex <= 0) {
      return windowStart;
    }

    Instant earliestAllowed = windowStart.minusSeconds(MAXIMUM_HEART_RATE_LAG_SECONDS);
    Instant minimumBackTarget = windowStart.minusSeconds(MINIMUM_HEART_RATE_LAG_SECONDS);

    int adjustedIndex = startIndex;

    // Step 1: unconditionally step back to at least the minimum lag.
    while (adjustedIndex > 0 && !samples.get(adjustedIndex - 1).timestamp.isBefore(minimumBackTarget)) {
      adjustedIndex--;
    }

    // Step 2: keep stepping back further while HR was still climbing (non-decreasing forward),
    // but never past the maximum lag cap.
    int checkingIndex = adjustedIndex;
    while (checkingIndex > 0
        && !samples.get(checkingIndex - 1).timestamp.isBefore(earliestAllowed)
        && samples.get(checkingIndex - 1).heartRate <= samples.get(checkingIndex).heartRate) {
      checkingIndex--;
      // we need a point right before first increase of the heart rate
      if (samples.get(checkingIndex).heartRate < samples.get(checkingIndex + 1).heartRate) adjustedIndex = checkingIndex;
    }

    return samples.get(adjustedIndex).timestamp;
  }

  private static int indexOfTimestamp(List<FitWorkout.HeartRateSample> samples, Instant timestamp) {
    for (int i = 0; i < samples.size(); i++) {
      if (samples.get(i).timestamp.equals(timestamp)) {
        return i;
      }
    }
    return -1;
  }

  private static String format(Duration duration) {
    long s = duration.getSeconds();
    return String.format("%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60);
  }

  private static HeartRateWindow getHeartRateWindow(Instant start, Duration duration, FitWorkout workout) {
    Instant end = start.plus(duration);

    List<FitWorkout.HeartRateSample> samples = workout.samples.stream()
        .filter(s -> s.heartRate != null && !s.timestamp.isBefore(start) && !s.timestamp.isAfter(end))
        .collect(Collectors.toList());

    if (samples.isEmpty()) {
      throw new IllegalStateException("No heart rate samples between " + start + " and " + end);
    }

    long sum = 0;
    for (FitWorkout.HeartRateSample sample : samples) {
      sum += sample.heartRate;
    }
    double average = (double) sum / samples.size();

    return new HeartRateWindow(samples.get(0).timestamp, samples.get(samples.size() - 1).timestamp, average, samples.size());
  }

  public static class HeartRateWindow {
    public final Instant start;
    public final Instant end;
    public final double averageHeartRate;
    public final int sampleCount;

    HeartRateWindow(Instant start, Instant end, double averageHeartRate, int sampleCount) {
      this.start = start;
      this.end = end;
      this.averageHeartRate = averageHeartRate;
      this.sampleCount = sampleCount;
    }
  }
}