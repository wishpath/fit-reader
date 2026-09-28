package org.sa;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConsolePrintUtil {

  private static final String RESET = "\u001B[0m";
  private static final String BOLD = "\u001B[1m";
  private static final String DIM = "\u001B[2m";
  private static final String GRAY = "\u001B[90m";
  private static final String CYAN = "\u001B[36m";
  private static final String MAGENTA = "\u001B[35m";
  private static final String GREEN = "\u001B[32m";
  private static final String YELLOW = "\u001B[33m";
  private static final String RED = "\u001B[31m";
  private static final String BLUE = "\u001B[34m";

  private static final DateTimeFormatter TIME_FMT =
      DateTimeFormatter.ofPattern("HH:mm:ss");
  private static final DateTimeFormatter DATE_FMT =
      DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy 'at' HH:mm:ss");

  public static void print(FitWorkout workout) {
    printHeader(workout);

    Map<Instant, SampleStats> stats = computeSampleStats(workout);

    List<Object> timeline = new ArrayList<>();
    timeline.addAll(workout.samples);
    timeline.addAll(workout.events);
    timeline.sort(Comparator.comparing(ConsolePrintUtil::timestampOf));

    for (Object item : timeline) {
      if (item instanceof FitWorkout.HeartRateSample) {
        FitWorkout.HeartRateSample sample = (FitWorkout.HeartRateSample) item;
        SampleStats s = stats.get(sample.timestamp);
        printSample(sample, s.paused, s.activeDuration);
      } else {
        printEvent((FitWorkout.WorkoutEvent) item);
      }
    }

    printEnded(workout);
  }

  /** Prints a list of HR samples using the same active-duration accounting as print(workout). */
  public static void printHeartRateSamples(List<FitWorkout.HeartRateSample> samples, FitWorkout workout) {
    Map<Instant, SampleStats> stats = computeSampleStats(workout);
    for (FitWorkout.HeartRateSample sample : samples) {
      SampleStats s = stats.get(sample.timestamp);
      boolean paused = s != null && s.paused;
      Duration activeDuration = s != null ? s.activeDuration : Duration.ZERO;
      printSample(sample, paused, activeDuration);
    }
  }

  /** Colored report header for the "hottest window" analysis result. */
  public static void printHottestWindowReport(AnalysisUtil.HeartRateWindow window, Duration requestedDuration, FitWorkout workout) {
    Map<Instant, SampleStats> stats = computeSampleStats(workout);
    Duration activeAtStart = activeDurationAt(stats, window.start);
    Duration activeAtEnd = activeDurationAt(stats, window.end);

    System.out.println();
    System.out.println(BOLD + YELLOW + "==================================================" + RESET);
    System.out.println(BOLD + YELLOW + " HOTTEST " + formatDurationLabel(requestedDuration) + " STRETCH" + RESET);
    System.out.println(BOLD + YELLOW + "==================================================" + RESET);
    System.out.println(CYAN + " From:     " + RESET + formatTime(window.start)
        + GRAY + "  (" + formatDate(window.start) + ")" + RESET
        + BLUE + "   [active " + formatElapsed(activeAtStart) + "]" + RESET);
    System.out.println(CYAN + " To:       " + RESET + formatTime(window.end)
        + GRAY + "  (" + formatDate(window.end) + ")" + RESET
        + BLUE + "   [active " + formatElapsed(activeAtEnd) + "]" + RESET);
    System.out.println(CYAN + " Avg HR:   " + RESET + BOLD + MAGENTA + String.format("%.1f bpm", window.averageHeartRate) + RESET);
    System.out.println(CYAN + " Samples:  " + RESET + window.sampleCount);
    System.out.println(BOLD + YELLOW + "==================================================" + RESET);
    System.out.println();
  }

  /**
   * Walks the full workout timeline once and, for every HR sample, records how much
   * *active* (non-paused) time had elapsed by that point, plus whether it fell inside
   * a paused stretch. This is the single source of truth both print(workout) and the
   * analysis report read from, so the numbers always agree.
   */
  private static Map<Instant, SampleStats> computeSampleStats(FitWorkout workout) {
    List<Object> timeline = new ArrayList<>();
    timeline.addAll(workout.samples);
    timeline.addAll(workout.events);
    timeline.sort(Comparator.comparing(ConsolePrintUtil::timestampOf));

    Map<Instant, SampleStats> stats = new LinkedHashMap<>();
    boolean paused = false;
    Instant lastTime = workout.startTime;
    Duration activeDuration = Duration.ZERO;

    for (Object item : timeline) {
      Instant itemTime = timestampOf(item);

      if (lastTime != null && !paused) {
        activeDuration = activeDuration.plus(Duration.between(lastTime, itemTime));
      }
      lastTime = itemTime;

      if (item instanceof FitWorkout.HeartRateSample) {
        stats.put(itemTime, new SampleStats(activeDuration, paused));
      } else {
        FitWorkout.WorkoutEvent event = (FitWorkout.WorkoutEvent) item;
        if (event.type == FitWorkout.EventType.PAUSED) {
          paused = true;
        } else if (event.type == FitWorkout.EventType.RESUMED) {
          paused = false;
        }
      }
    }

    return stats;
  }

  private static Duration activeDurationAt(Map<Instant, SampleStats> stats, Instant timestamp) {
    SampleStats s = stats.get(timestamp);
    return s != null ? s.activeDuration : Duration.ZERO;
  }

  private static class SampleStats {
    final Duration activeDuration;
    final boolean paused;

    SampleStats(Duration activeDuration, boolean paused) {
      this.activeDuration = activeDuration;
      this.paused = paused;
    }
  }

  private static String formatElapsed(Duration duration) {
    long s = duration.getSeconds();
    return String.format("%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60);
  }

  private static String formatDurationLabel(Duration duration) {
    long totalSeconds = duration.getSeconds();
    long hours = totalSeconds / 3600;
    long minutes = (totalSeconds % 3600) / 60;
    long seconds = totalSeconds % 60;

    StringBuilder sb = new StringBuilder();
    if (hours > 0) sb.append(hours).append("h ");
    if (minutes > 0 || hours > 0) sb.append(minutes).append("m ");
    sb.append(seconds).append("s");
    return sb.toString().trim();
  }

  private static Instant timestampOf(Object item) {
    if (item instanceof FitWorkout.HeartRateSample) {
      return ((FitWorkout.HeartRateSample) item).timestamp;
    }
    return ((FitWorkout.WorkoutEvent) item).timestamp;
  }

  private static void printHeader(FitWorkout workout) {
    String dateStr = workout.startTime != null ? formatDate(workout.startTime) : "unknown date";

    System.out.println();
    System.out.println(BOLD + CYAN + "==================================================" + RESET);
    System.out.println(BOLD + CYAN + " Workout: " + workout.sportName + RESET);
    System.out.println(BOLD + CYAN + " Date:    " + dateStr + RESET);
    System.out.println(BOLD + CYAN + "==================================================" + RESET);
    System.out.println(BOLD + GREEN + ">> WORKOUT STARTED" + RESET);
    System.out.println();
  }

  private static void printSample(FitWorkout.HeartRateSample sample, boolean paused, Duration activeDuration) {
    String time = formatTime(sample.timestamp);
    String hr = sample.heartRate != null ? String.valueOf(sample.heartRate) : "--";

    if (paused) {
      System.out.println(GRAY + time + RESET + "   " + DIM + GRAY + "HR " + hr + " bpm  (paused)" + RESET);
    } else {
      String active = formatElapsed(activeDuration);
      System.out.println(GRAY + time + RESET + "  " + BLUE + "active " + active + RESET
          + "   " + MAGENTA + "HR " + hr + " bpm" + RESET);
    }
  }

  private static void printEvent(FitWorkout.WorkoutEvent event) {
    String time = formatTime(event.timestamp);
    System.out.println();
    switch (event.type) {
      case RESUMED:
        System.out.println(BOLD + GREEN + ">> RESUMED  at " + time + RESET);
        break;
      case PAUSED:
        System.out.println(BOLD + YELLOW + "|| PAUSED   at " + time + RESET);
        break;
      default:
        System.out.println(BOLD + BLUE + "** EVENT  at " + time + RESET);
    }
    System.out.println();
  }

  private static void printEnded(FitWorkout workout) {
    if (workout.endTime == null) {
      return;
    }
    System.out.println();
    System.out.println(BOLD + RED + "[END] WORKOUT ENDED  at " + formatTime(workout.endTime) + RESET);
  }

  private static String formatTime(Instant instant) {
    return TIME_FMT.format(instant.atZone(ZoneId.systemDefault()));
  }

  private static String formatDate(Instant instant) {
    return DATE_FMT.format(instant.atZone(ZoneId.systemDefault()));
  }
}