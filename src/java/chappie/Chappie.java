package chappie;

import chappie.naive.NaiveAttributionModule;
import chappie.profiling.Profile;
import chappie.profiling.Profiler;
import dagger.Component;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;

// temporary driver; should be able to get rid of this in some way or another
public final class Chappie {
  @Component(modules = NaiveAttributionModule.class)
  interface ProfilerFactory {
    Profiler newProfiler();
  }

  private static Profiler profiler;
  private static Iterable<Profile> profiles = new ArrayList<Profile>();

  // starts a profiler if there is not one already
  public static void start() {
    if (profiler == null) {
      profiles = new ArrayList<Profile>();
      profiler = DaggerChappie_ProfilerFactory.builder().build().newProfiler();
      profiler.start();
    }
  }

  // stops the profiler if there is one
  public static void stop() {
    if (profiler != null) {
      profiler.stop();
      profiles = profiler.getProfiles();
      profiler = null;
    }
  }

  // get all profiles stored
  public static Iterable<Profile> getProfiles() {
    if (profiler != null) {
      profiles = profiler.getProfiles();
    }
    return profiles;
  }

  private Chappie() { }
}
