package chappie.util;

import java.util.concurrent.atomic.AtomicIntegerArray;

public class Histogram<T extends Comparable> {
  private final long[] values;
  private final AtomicIntegerArray counts;

  public Histogram(long[] data, long start, long end, int bins) {
    values = new long[bins + 1];
    counts = new AtomicIntegerArray(bins + 1);

    for (int i = 0; i < values.length; i++) {
      values[i] = (end - start) / bins * i + start;
    }

    for (long value: data) {
      for (int i = 0; i < values.length; i++) {
        if (value > values[i] && i + 1 < values.length) {
          continue;
        } else {
          counts.getAndIncrement(i);
          break;
        }
      }
    }
  }

  public long[] getBins() {
    return values;
  }

  public int[] getCounts() {
    int[] counts = new int[this.counts.length()];
    for (int i = 0; i < counts.length; i++) {
      counts[i] = this.counts.get(i);
    }
    return counts;
  }

  @Override
  public String toString() {
    String message = "[";
    for (int i = 0; i < values.length; i++) {
      message += counts.get(i) + ",";
    }
    return message.substring(0, message.length() - 1) + "]";
  }
}
