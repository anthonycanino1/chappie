package chappie.processing.trace;

import chappie.sampling.trace.StackTraceSet;
import chappie.sampling.trace.StackTraceSample;
import chappie.profiling.Sample;
import chappie.profiling.SampleProcessor;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

/** Comparable collection of stack traces to evaluate correlation. */
class StackTraceSorter implements SampleProcessor<Iterable<Entry<Instant, List<StackTraceSample>>>> {
  private TreeMap<Instant, List<StackTraceSample>> stackData = new TreeMap<>();

  public StackTraceSorter() { }

  /** Adds to a trace's ranking value. */
  @Override
  public void add(Sample s) {
    if (s instanceof StackTraceSet) {
      synchronized (stackData) {
        for (Sample sample: ((StackTraceSet) s).getSamples()) {
          StackTraceSample stack = (StackTraceSample) sample;
          this.stackData.putIfAbsent(stack.getTimestamp(), new ArrayList<StackTraceSample>());
          this.stackData.get(stack.getTimestamp()).add(stack);
        }
      }
    }
  }

  @Override
  public Iterable<Entry<Instant, List<StackTraceSample>>> process() {
    Set<Entry<Instant, List<StackTraceSample>>> stackTraces = new TreeMap().entrySet();
    synchronized (stackData) {
      stackTraces = stackData.entrySet();
      stackData = new TreeMap<>();
    }
    return stackTraces;
  }
}
