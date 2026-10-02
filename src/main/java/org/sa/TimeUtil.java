package org.sa;

import java.time.Duration;
import java.time.Instant;

public class TimeUtil {
  public static Duration parseDuration(String text) {
    String[] parts = text.trim().split(":");
    try {
      long hours = 0, minutes = 0, seconds;
      switch (parts.length) {
        case 3:
          hours = Long.parseLong(parts[0]);
          minutes = Long.parseLong(parts[1]);
          seconds = Long.parseLong(parts[2]);
          break;
        case 2:
          minutes = Long.parseLong(parts[0]);
          seconds = Long.parseLong(parts[1]);
          break;
        case 1:
          seconds = Long.parseLong(parts[0]);
          break;
        default:
          throw new NumberFormatException();
      }
      return Duration.ofHours(hours).plusMinutes(minutes).plusSeconds(seconds);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          "Could not parse duration '" + text + "'. Expected 'HH:mm:ss', 'mm:ss', or seconds.");
    }
  }

  /**
   returns a standard Unix timestamp—the total number of seconds elapsed since January 1, 1970 00:00:00 UTC
  */
  public static long toEpochSecond(Instant startTime, String offsetText) {
    return startTime.plus(TimeUtil.parseDuration(offsetText)).getEpochSecond();
  }

}
