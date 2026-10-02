package org.sa.APPS;

import org.sa.AnalysisUtil;
import org.sa.FileUtil;
import org.sa.FitParseUtil;
import org.sa.FitWorkout;

/**
 * Entry point that finds and prints the hardest stretch of a workout.
 * Reads the single .fit file from resources, parses it into a FitWorkout,
 * and prints the highest-average-heart-rate window of the given duration
 * along with every heart-rate sample within it.
 */
public class HottestSegmentFinder {
  public static void main(String[] args) throws Exception {
    byte[] fitData = FileUtil.loadSingleFitFile();
    FitWorkout workout = FitParseUtil.parse(fitData);
    AnalysisUtil.printHottestHeartRateWindow(workout, "30:00");
  }
}