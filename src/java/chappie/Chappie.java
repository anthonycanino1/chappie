package chappie;

import chappie.attribution.AttributionProfiler;
import chappie.attribution.processing.EnergyAttribution;
import chappie.util.profiling.Profile;
import chappie.util.profiling.Profiler;
import chappie.util.logging.ChappieLogger;
import java.util.ArrayList;
import java.util.logging.Logger;

public final class Chappie {
  private static Logger logger;
  private static Profiler profiler;
  private static ArrayList<Profile> profiles;

  public static void start() {
    ChappieLogger.setup();
    logger = Logger.getLogger("chappie");

    logger.info("starting attribution profiling");
    int rate = Integer.parseInt(System.getProperty("chappie.rate", "32"));
    profiles = new ArrayList<>();
    profiler = new AttributionProfiler(rate, rate / 2);
    profiler.start();
  }

  public static void stop() {
    logger.info("stopping attribution profiling");
    profiler.stop();
    for (Profile profile: profiler.getProfiles()) {
      profiles.add(profile);
    }

    profiler = null;
  }

  public static Iterable<Profile> getProfiles() {
    if (profiler != null) {
      profiler.stop();
      for (Profile profile: profiler.getProfiles()) {
        profiles.add(profile);
      }
      profiler.start();
    }
    return profiles;
  }

  private Chappie() { }
}
