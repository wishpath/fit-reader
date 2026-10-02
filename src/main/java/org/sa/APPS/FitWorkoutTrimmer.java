package org.sa.APPS;

import com.garmin.fit.Decode;
import com.garmin.fit.Field;
import com.garmin.fit.FileEncoder;
import com.garmin.fit.Fit;
import com.garmin.fit.Mesg;
import com.garmin.fit.MesgBroadcaster;
import com.garmin.fit.MesgListener;
import org.sa.FileUtil;
import org.sa.FitParseUtil;
import org.sa.FitWorkout;
import org.sa.TimeUtil;

import java.io.ByteArrayInputStream;

/**
 * Entry point that trims a workout by start/end times relative to the beginning
 * and saves a new .fit file to output directory.
 */
public class FitWorkoutTrimmer {
  public static void main(String[] args) throws Exception {
    // 1. load workout
    byte[] rawFitBytes = FileUtil.loadSingleFitFile();
    FitWorkout workout = FitParseUtil.parse(rawFitBytes);

    long trimStartEpoch = TimeUtil.toEpochSecond(workout.startTime, "00:00:00");
    long trimEndEpoch = TimeUtil.toEpochSecond(workout.startTime, "01:30:00");

    // 2. set filter
    MesgBroadcaster filter = new MesgBroadcaster(new Decode());
    String outputFileName = FileUtil.getSingleFitFileName().replace(".fit", "_TRIM_" + java.time.LocalDate.now() + ".fit");
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