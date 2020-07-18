package chappie.profiling;

/** Interface that returns a {@link Sample}. This is almost certainl vestigal and I haven't figured out the correct design yet. */
public interface Sampler<S extends Sample> {
  /** Returns a sample. */
  public S sample();
}
