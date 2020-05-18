// package chappie.attribution;
//
// import chappie.attribution.task.TaskSampleSet;
// import chappie.attribution.trace.StackTrace;
// import chappie.attribution.trace.StackTraceSampleSet;
// import chappie.util.MathUtil;
// import chappie.util.profiling.Profile;
// import java.util.Arrays;
// import java.util.ArrayList;
// import java.util.Comparator;
// import java.util.HashMap;
// import java.util.Map;
// import java.util.Map.Entry;
// import java.util.Set;
// import java.util.stream.Collectors;
//
// /** Comparable collection of stack traces to evaluate correlation. */
// public class StackTraceRanking implements Profile {
//   /** merges some number of rankings into a single ranking. */
//   static StackTraceRanking merge(StackTraceRanking... rankings) {
//     return merge(Arrays.asList(rankings));
//   }
//
//   static StackTraceRanking merge(Iterable<StackTraceRanking> rankings) {
//     StackTraceRanking merged = new StackTraceRanking();
//     for (StackTraceRanking ranking: rankings) {
//       merged = merged.merge(ranking);
//     }
//
//     return merged;
//   }
//
//   private final HashMap<StackTrace, Double> values = new HashMap<>();
//   private final HashMap<StackTrace, Integer> counts = new HashMap<>();
//
//   /** Returns the percent of total machine power consumed. */
//   @Override
//   public double evaluate() {
//     // double attributed = 0;
//     // double total = 0;
//     // for (int i = 0; i < this.total.length; i++) {
//     //   attributed += this.attributed[i];
//     //   total += this.total[i];
//     // }
//     // if (total == 0) {
//       return 0;
//     // } else {
//     //   return attributed / total;
//     // }
//   }
//
//   /**
//   * Computes the pearson's correlation between the two rankings (stack trace
//   * as index).
//   */
//   @Override
//   public double compare(Profile other) {
//     if (other instanceof StackTraceRanking) {
//       return MathUtil.pcc(this.values, ((StackTraceRanking) other).values);
//     } else {
//       return 0;
//     }
//   }
//
//   @Override
//   public String dump() {
//     return values
//       .entrySet()
//       .stream()
//       .sorted(Entry.<StackTrace, Double>comparingByValue(Comparator.reverseOrder()))
//       .limit(10)
//       .map(entry -> entry.getKey() + "," + entry.getValue())
//       .collect(Collectors.joining("\n"));
//   }
//
//   @Override
//   public String toString() {
//     if (!values.isEmpty()) {
//       double sum = values.values()
//         .stream()
//         .mapToDouble(Double::valueOf)
//         .sum();
//       return values
//         .entrySet()
//         .stream()
//         .sorted(Entry.<StackTrace, Double>comparingByValue(Comparator.reverseOrder()))
//         .limit(10)
//         .map(entry -> entry.getKey() + " - " + String.format("%.2f", entry.getValue()) + "J (" + String.format("%.2f", 100 * entry.getValue() / sum) + "%)")
//         .collect(Collectors.joining("\n"));
//     } else {
//       return "";
//     }
//   }
//
//   StackTraceRanking() { }
//
//   StackTraceRanking(StackTraceSampleSet trace, TaskSampleSet tasks, EnergyAttribution attribution) {
//     Set<Integer> alignableTasks = tasks.getTaskIds();
//     alignableTasks.retainAll(trace.getTaskIds());
//
//     for (int id: alignableTasks) {
//       this.add(
//         trace.getStackTrace(id).stripUntil(StackTrace::isApplicationMethod).getCallingMethod(),
//         attribution.getTaskAttributed(tasks.getSocket(id)));
//     }
//   }
//
//   /** Combines all traces in the two rankings and returns a new ranking. */
//   private StackTraceRanking merge(StackTraceRanking other) {
//     StackTraceRanking ranking = new StackTraceRanking();
//     for (StackTrace trace: this.values.keySet()) {
//       ranking.add(trace, this.values.get(trace));
//     }
//     for (StackTrace trace: other.values.keySet()) {
//       ranking.add(trace, other.values.get(trace));
//     }
//
//     return ranking;
//   }
//
//   /** Adds to a trace's ranking value. */
//   private void add(StackTrace trace, double value) {
//     if (trace.length > 0 && value > 0) {
//       values.putIfAbsent(trace, 0.0);
//       values.put(trace, values.get(trace) + value);
//       counts.putIfAbsent(trace, 0);
//       counts.put(trace, counts.get(trace) + 1);
//     }
//   }
// }
