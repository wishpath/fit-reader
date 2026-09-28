# FIT Workout Reader
## What it does
- Parses a `.fit` file (e.g. a CrossFit workout exported from Strava).
- Locates the hardest stretch of a given duration (your "quality" working part,
  as opposed to warmup/rest) and reports its average heart rate and time range.

## Requirements
- JDK 17+ (or any version supported by the Gradle version in `gradle-wrapper.properties`)

## Run instructions
1. Drop exactly one `.fit` file into `src/main/resources/`.
2. In `Main.java`, set the duration of your quality part in `HH:mm:ss` format,
   e.g. `AnalysisUtil.printHottestHeartRateWindow(workout, "00:30:00")`.
3. Run it: `./gradlew run` (or run `Main.main()` from your IDE).