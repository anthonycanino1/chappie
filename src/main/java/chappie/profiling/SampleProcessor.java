package chappie.profiling;

/** Interface that takes in samples and produces a specified type. */
public interface SampleProcessor<T> {
  /** Adds a sample to the processor's data. */
  void add(Sample s);

  /** Returns the result of the processor from the input data. Error checking should be handled by the caller. */
  T process();
}
