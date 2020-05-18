package chappie.util.profiling;

/** Simple interface for a profiler. */
public interface Profiler {
  public void start();
  public void stop();
  public Iterable<Profile> getProfiles();
}
