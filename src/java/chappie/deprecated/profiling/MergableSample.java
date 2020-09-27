package chappie.profiling;

import chappie.profiling.Sample;

/** Interface for a sample that can be combined with other samples. */
public interface MergableSample<S extends Sample> extends Sample {
  S merge(S other);
}
