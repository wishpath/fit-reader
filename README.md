# .fit Workout Reader

## Parses .fit
- Parses a `.fit` file (e.g. a CrossFit workout exported from Strava).

## Prints
- prints every heart-rate sample alongside pause/resume/end markers to console.
  - HOW TO
    - run FitWorkoutPrinter.main()

## Finds highest-average-heart-rate window
- finds highest-average-heart-rate window of the given duration (opposed to warmup and cooldown)
- calculates average heart-rate for this window
- prints info
  - HOW TO:
    - go to HottestSegmentFinder class
    - hardcode given duration into this line `AnalysisUtil.printHottestHeartRateWindow(workout, "00:30:00")`
    - run HottestSegmentFinder.main()

## Trims .fit
- trims workout (beginning and end) and outputs new .fit in output directory.
- HOW TO:
  - go to FitWorkoutTrimmer
  - hardcode start and end times 
    - relative time that passed since the workout beginning
  - run FitWorkoutTrimmer.main()