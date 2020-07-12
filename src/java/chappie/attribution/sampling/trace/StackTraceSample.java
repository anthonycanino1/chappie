package chappie.attribution.sampling.trace;

import chappie.profiling.TimestampedSample;
import java.time.Instant;

public final class StackTraceSample implements TimestampedSample {
  private final Instant timestamp;
  private final int pid;
  private final StackTrace stackTrace;

  StackTraceSample(String asyncRecord) {
    String[] values = asyncRecord.split(",");
    timestamp = Instant.ofEpochMilli(Long.parseLong(values[0]));
    pid = Integer.parseInt(values[1]);
    stackTrace = new StackTrace(values[2]);
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
