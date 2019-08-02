package chappie.profile;

public interface Profiler {
  public Profiler(Config config);

  public void sample(int epoch, long epochTime);

  private void dump();
}
