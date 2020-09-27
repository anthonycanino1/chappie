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
class AsyncRecordProcessor implements Processor<String, Iterable<Entry<Instant, Iterable<String>>>> {
  private TreeMap<Instant, Iterable<String>> traces = new TreeMap<>();

  AsyncRecordProcessor() { }

  /** Adds to a trace's ranking value. */
  @Override
  public void add(String asyncRecords) {
    for (String record: asyncRecords.split("\n")) {
      String[] values = record.split(",");
      timestamp = Instant.ofEpochMilli(Long.parseLong(values[0]));
      trace = values[2];
      synchronized (traces) {
        this.stackData.putIfAbsent(timestamp, new ArrayList<StackTraceSample>());
        this.stackData.get(timestamp).add(trace);
      }
    }
  }

  @Override
  public Iterable<Entry<Instant, Iterable<String>>> process() {
    Set<Entry<Instant, Iterable<String>>> stackTraces = new TreeMap<>().entrySet();
    synchronized (stackData) {
      stackTraces = traces.entrySet();
      stackData = new TreeMap<>();
    }
    return stackTraces;
  }
}
