package chappie.data;

import eflect.data.Sample;
import java.time.Instant;

public final class AsyncProfilerSample implements Sample {
  // fix this; probably should be module-based as well
  private static final Duration asyncRate = Duration.ofMillis(1);
  private static final boolean noAsync = !init();
  private static final ArrayList<StackTraceSet> traces = new ArrayList<>();

  /** Set up and run the async-profiler. Returns if the profiler has started. */
  private static boolean setupAsync() {
    Logger logger = LoggerUtil.setup();
    try {
      // TODO(timur): this is pretty crappy; it only works if you ran from where chappie's jar is
      long rate = asyncRate.getNano(); // only supporting sub-second for the moment
      AsyncProfiler.getInstance().start(Events.CPU, asyncRate.getNano());
      logger.info("started async-profiler at " + asyncRate);
      return true;
    } catch (Exception e) {
      logger.log(WARNING, "unable to start async-profiler", e);
      return false;
    }
  }

  /**
  * Returns the records dumped from the async-profiler as a stack trace sample
  * set. The original data is delimited as id;ts;stck\nid;ts;stck, so our other
  * data structures will handle them as needed.
  */
  private static ArrayList<String> sampleStackTraces() {
    ArrayList<String> traces = new ArrayList<>();
    if (!noAsync) {
      AsyncProfiler.getInstance().stop();
      for (String trace: AsyncProfiler.getInstance().dumpRecords().split("\n")) {
        if (trace.length() > 0) {
          traces.add(trace);
        }
      }
      AsyncProfiler.getInstance().resume(Events.CPU, asyncRate.getNano());
    }
    return traces;
  }

  private final String stackTrace;
  private final Instant timestamp;

  StackTraceSample(String asyncRecord) {
    String[] values = asyncRecord.split(",");
    timestamp = Instant.ofEpochMilli(Long.parseLong(values[0]));
    stackTrace = values[2];
  }

  @Override
  public Instant getTimestamp() {
    return timestamp;
  }

  public int getCallerId() {
    return pid;
  }

  public StackTrace getStackTrace() {
    return stackTrace;
  }
}
