package chappie.profiling;

/** Interface for an object that produces {@link Profile}s. */
public interface Profiler<P extends Object> {
  /** Starts collecting profiles. */
  public void start();

  /** Stops collecting profiles. */
  public void stop();

  /** Returns any profiles collected. */
  public Iterable<P> getProfiles();
}
