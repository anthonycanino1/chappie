package chappie.profiling;

import chappie.profiling.Sample;

/** Interface for a sample containing multiple samples. */
public interface SampleSet<S extends Sample> extends Sample {
  Iterable<S> getSamples();
}
