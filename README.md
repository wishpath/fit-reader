# .fit Workout Reader

## Parses .fit
- Parses a `.fit` file (e.g. a CrossFit workout exported from Strava).

## Prints .fit info
- prints every heart-rate sample alongside pause/resume/end markers to console.
- HOW TO:
  - run **FitWorkoutPrinter**.main()

## Finds highest-average-heart-rate window
- finds highest-average-heart-rate window of the given duration (opposed to warmup and cooldown)
- calculates average heart-rate for this window
- HOW TO:
  - hardcode "hottest window" duration
  - run **HottestSegmentFinder**.main()

## Trims .fit
- trims workout (beginning and end) and outputs new .fit in output directory.
- HOW TO:
  - hardcode start and end times
  - run **FitWorkoutTrimmer**.main()