package chappie;

import static java.util.Collections.emptyMap;

import one.profiler.AsyncProfiler;
import clerk.Clerk;
import clerk.Profiler;
import clerk.concurrent.PeriodicSamplingModule;
import dagger.Component;
import java.io.File;
import java.util.Map;

/** A profiler that estimates the energy consumed by the current application. */
public final class ChappieProfiler implements Profiler<Map<String, Double>> {
  @Component(modules = {StackTraceRankingModule.class, PeriodicSamplingModule.class})
  interface ClerkFactory {
    Clerk<Map<String, Double>> newClerk();
  }

  private static final ClerkFactory clerkFactory = DaggerChappieProfiler_ClerkFactory.builder().build();

  private Clerk<Map<String, Double>> clerk;

  public ChappieProfiler() { }

  // starts a profiler if there is not one
  public void start() {
    if (clerk == null) {
      clerk = clerkFactory.newClerk();
      clerk.start();
    }
  }

  // stops the profiler if there is one
  public Map<String, Double> stop() {
    Map<String, Double> profiles = emptyMap();
    if (clerk != null) {
      profiles = (Map<String, Double>) clerk.stop();
      clerk = null;
    }
    return profiles;
  }

  public static void main(String[] args) throws Exception {
    String pid = args[0];
    File procPid = new File("/proc", args[0]);

    ChappieProfiler chappie = new ChappieProfiler();
    chappie.start();
    while (procPid.exists()) { }
    System.out.println(chappie.stop());
  }
}
