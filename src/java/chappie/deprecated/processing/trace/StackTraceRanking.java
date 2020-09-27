package chappie.processing.trace;

import static java.util.Collections.unmodifiableMap;

import chappie.sampling.trace.StackTrace;
import chappie.util.MathUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/** Comparable collection of stack traces to evaluate correlation. */
public class StackTraceRanking {
  private final Map<StackTrace, Double> values;
  private final Map<StackTrace, Integer> counts;

  private StackTraceRanking(Map<StackTrace, Double> values, Map<StackTrace, Integer> counts) {
    this.values = values;
    this.counts = counts;
  }

  /** Computes the pcc between two rankings by stack trace. */
  public double compare(StackTraceRanking other) {
    return MathUtil.pcc(this.values, other.values);
  }

  public String dump() {
    return values
      .entrySet()
      .stream()
      .sorted(Entry.<StackTrace, Double>comparingByValue(Comparator.reverseOrder()))
      .map(entry -> entry.getKey() + "," + entry.getValue())
      .collect(Collectors.joining("\n"));
  }

  @Override
  public String toString() {
    if (!values.isEmpty()) {
      double sum = values.values()
        .stream()
        .mapToDouble(Double::valueOf)
        .sum();
      return values
        .entrySet()
        .stream()
        .sorted(Entry.<StackTrace, Double>comparingByValue(Comparator.reverseOrder()))
        .limit(10)
        .map(entry -> entry.getKey() + " - " + String.format("%.2f", entry.getValue()) + "J (" + String.format("%.2f", 100 * entry.getValue() / sum) + "%)")
        .collect(Collectors.joining("\n"));
    } else {
      return "no rankings";
    }
  }

  static class Builder {
    private final HashMap<StackTrace, Double> values = new HashMap<>();
    private final HashMap<StackTrace, Integer> counts = new HashMap<>();

    /** Adds to a trace's ranking value. */
    void add(StackTrace trace, double value) {
      if (trace.length > 0 && value > 0) {
        values.putIfAbsent(trace, 0.0);
        values.put(trace, values.get(trace) + value);
        counts.putIfAbsent(trace, 0);
        counts.put(trace, counts.get(trace) + 1);
      }
    }

    StackTraceRanking build() {
      return new StackTraceRanking(unmodifiableMap(values), unmodifiableMap(counts));
    }
  }
}
