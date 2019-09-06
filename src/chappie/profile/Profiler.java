package chappie.profile;

import chappie.Chaperone.Config;

public abstract class Profiler {
  protected Config config;
  public Profiler(Config config) { this.config = config; }

  public abstract void sample(int epoch, long timestamp);

  public abstract void dump();
}
