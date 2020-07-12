package chappie.attribution.processing;

import static java.util.Collections.unmodifiableMap;

import chappie.attribution.StackTraceRanking;
import chappie.attribution.sampling.trace.StackTrace;
import chappie.profiling.Profile;
import chappie.util.MathUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/** Comparable collection of stack traces to evaluate correlation. */
public class RankingBuilder {

  private final HashMap<StackTrace, Double> values = new HashMap<>();
  private final HashMap<StackTrace, Integer> counts = new HashMap<>();

  RankingBuilder() { }

  /** Adds to a trace's ranking value. */
  void add(StackTrace trace, double value) {
    if (trace.length > 0 && value > 0) {
      values.putIfAbsent(trace, 0.0);
      values.put(trace, values.get(trace) + value);
      counts.putIfAbsent(trace, 0);
      counts.put(trace, counts.get(trace) + 1);
    }
  }

  StackTraceRanking buildRanking() {
    return new StackTraceRanking(unmodifiableMap(values), unmodifiableMap(counts));
  }
}
