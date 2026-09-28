package org.sa;

public class Main {
  public static void main(String[] args) throws Exception {
    byte[] fitData = FileUtil.loadSingleFitFile();
    FitWorkout workout = FitParseUtil.parse(fitData);
    //ConsolePrintUtil.print(workout);
    AnalysisUtil.printHottestHeartRateWindow(workout, "30:00");
  }
}