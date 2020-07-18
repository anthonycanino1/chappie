package chappie.profiling;

import chappie.profiling.Sample;
import java.time.Instant;

/** Interface for a timestamped sample. */
public interface TimestampedSample extends Sample {
  Instant getTimestamp();
}
