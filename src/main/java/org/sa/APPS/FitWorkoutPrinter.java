package org.sa.APPS;

import org.sa.ConsolePrintUtil;
import org.sa.FileUtil;
import org.sa.FitParseUtil;
import org.sa.FitWorkout;

/**
 * Entry point that prints the full workout log to the console.
 * Reads the single .fit file from resources, parses it into a FitWorkout,
 * and prints every heart-rate sample alongside pause/resume/end markers —
 * the complete session, not just a summary.
 */
public class FitWorkoutPrinter {
  public static void main(String[] args) throws Exception {
    byte[] fitData = FileUtil.loadSingleFitFile();
    FitWorkout workout = FitParseUtil.parse(fitData);
    ConsolePrintUtil.print(workout);
  }
}
