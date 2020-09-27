package chappie.util;

import java.time.Instant;

public final class TimeUtil {
  // equality
  public static boolean atMost(Instant first, Instant second) {
    return first.compareTo(second) <= 0;
  }

  public static boolean atLeast(Instant first, Instant second) {
    return first.compareTo(second) >= 0;
  }

  public static boolean equal(Instant first, Instant second) {
    return first.compareTo(second) == 0;
  }

  public static boolean greaterThan(Instant first, Instant second) {
    return first.compareTo(second) > 0;
  }

  public static boolean lessThan(Instant first, Instant second) {
    return first.compareTo(second) < 0;
  }

  // comparisons
  public static Instant max(Instant first, Instant second) {
    if (greaterThan(first, second)) {
      return first;
    } else {
      return second;
    }
  }

  public static Instant max(Instant first, Instant... others) {
    Instant minTimestamp = first;
    for (Instant other: others) {
      minTimestamp = max(minTimestamp, other);
    }
    return minTimestamp;
  }

  public static Instant maxBelowUpper(Instant first, Instant second) {
    if ((greaterThan(first, second) && !equal(first, Instant.MAX)) || equal(second, Instant.MAX)) {
      return first;
    } else {
      return second;
    }
  }

  public static Instant min(Instant first, Instant second) {
    if (lessThan(first, second)) {
      return first;
    } else {
      return second;
    }
  }

  public static Instant min(Instant first, Instant... others) {
    Instant minTimestamp = first;
    for (Instant other: others) {
      minTimestamp = min(minTimestamp, other);
    }
    return minTimestamp;
  }

  public int parseToMillis(String time) {
    return 0;
  }

  public static Instant minAboveLower(Instant first, Instant second) {
    if ((lessThan(first, second) && !equal(first, Instant.MIN)) || equal(second, Instant.MIN)) {
      return first;
    } else {
      return second;
    }
  }
}
