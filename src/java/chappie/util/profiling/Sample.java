package chappie.util.profiling;

import java.time.Instant;

/** Simple interface for raw data. */
public interface Sample {
  // this is not guaranteed to be immutable
  Sample merge(Sample other);

  Instant getTimestamp();
}
