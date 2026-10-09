package org.sa.APPS;

import com.garmin.fit.*;
import org.sa.FileUtil;
import org.sa.FitParseUtil;
import org.sa.FitWorkout;
import org.sa.TimeUtil;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Entry point that trims a workout by start/end times relative to the beginning
 * and saves a new .fit file to output directory.
 */
public class FitWorkoutTrimmer {
  private static final String TRIM_START = "00:00:00";
  private static final String TRIM_END = "01:30:00";


  public static void main(String[] args) throws Exception {
    // 1. load workout
    byte[] rawFitBytes = FileUtil.loadSingleFitFile();
    FitWorkout workout = FitParseUtil.parse(rawFitBytes);

    validateTrimTimeMarkers(workout);
    long trimStartEpoch = TimeUtil.toEpochSecond(workout.startTime, TRIM_START);
    long trimEndEpoch = TimeUtil.toEpochSecond(workout.startTime, TRIM_END);

    // 2. set filter
    MesgBroadcaster filter = new MesgBroadcaster(new Decode());
    String outputFileName = getOutputFileName(workout);
    FileEncoder allowsWriting = new FileEncoder(FileUtil.createEmptyOutputFile(outputFileName), Fit.ProtocolVersion.V2_0);
    filter.addListener((MesgListener) dataEntry -> {
      if (shouldDropMessage(dataEntry, trimStartEpoch, trimEndEpoch)) return; // filter out
      allowsWriting.onMesg(dataEntry); // include and write
    });

    // 3. write
    try (ByteArrayInputStream rawByteStream = new ByteArrayInputStream(rawFitBytes)) {
      filter.run(rawByteStream);
    }
    allowsWriting.close();
  }

  private static void validateTrimTimeMarkers(FitWorkout workout) {
    Duration workoutDuration = Duration.between(workout.startTime, workout.endTime);
    if (workoutDuration.compareTo(TimeUtil.parseDuration(TRIM_END)) < 0) {
      throw new IllegalArgumentException("TRIM_END should be before the workout ends");
    }
    if (workoutDuration.compareTo(TimeUtil.parseDuration(TRIM_START)) < 0) {
      throw new IllegalArgumentException("TRIM_START should be before the workout ends");
    }
  }

  private static String getOutputFileName(FitWorkout workout) {
    String workoutDate = DateTimeFormatter.ofPattern("yyyy_MM_dd").format(workout.startTime.atZone(ZoneId.systemDefault()));
    String start = TRIM_START.replace(":", "-");
    String end = TRIM_END.replace(":", "-");
    String suffix = "_TRIMMED_from_" + start + "_to_" + end + ".fit";
    return workoutDate + "_" + FileUtil.getSingleFitFileName().replace(".fit", suffix);
  }

  private static boolean shouldDropMessage(Mesg message, long trimStartEpoch, long trimEndEpoch) {
    // Header & metadata messages (file_id, session, sport) carry no "timestamp" field and MUST be kept
    if (!"record".equals(message.getName()) && !"event".equals(message.getName())) {
      return false;
    }
    Field timestampField = message.getField("timestamp");
    if (timestampField == null || timestampField.getLongValue() == null) {
      return false;
    }
    // FIT epoch starts on 1989-12-31T00:00:00Z; add 631065600 seconds offset to match Unix epoch
    long sampleUnixTimestamp = timestampField.getLongValue() + 631065600L;
    return sampleUnixTimestamp < trimStartEpoch || sampleUnixTimestamp > trimEndEpoch;
  }
}