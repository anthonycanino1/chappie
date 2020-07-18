package chappie;

import chappie.naive.NaiveAttributionModule;
import chappie.profiling.Profile;
import chappie.profiling.Profiler;
import chappie.util.LoggerUtil;
import dagger.Component;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.logging.Logger;

// temporary driver; should be able to get rid of this in some way or another
public final class Chappie {
  @Component(modules = NaiveAttributionModule.class)
  interface ProfilerFactory {
    Profiler newProfiler();
  }

  private static Logger logger;
  private static Profiler profiler;
  private static Iterable<Profile> profiles = new ArrayList<Profile>();

  // starts a profiler if there is not one already
  public static void start() {
    if (profiler == null) {
      logger = LoggerUtil.setup();
      profiles = new ArrayList<Profile>();
      profiler = DaggerChappie_ProfilerFactory.builder().build().newProfiler();

      logger.info("starting attribution profiling");
      profiler.start();
    }
  }

  // stops the profiler if there is one
  public static void stop() {
    if (profiler != null) {
      logger.info("stopping attribution profiling");
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

  // write all profiles as csv to the path
  public static void dumpProfiles(String path) {
    try (PrintWriter writer = new PrintWriter(new FileWriter(path))) {
      writer.println("start,end,socket,total,attributed");
      for (Profile profile: Chappie.getProfiles()) {
        writer.println(profile.dump());
      }
    } catch (Exception e) {
      System.out.println("unable to write profiles");
      e.printStackTrace();
    }
  }

  private Chappie() { }
}
