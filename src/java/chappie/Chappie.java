package chappie;

import static java.util.Collections.emptyList;

import clerk.Profiler;
import clerk.Clerk;
import clerk.concurrent.PeriodicSamplingModule;
import dagger.Component;
import java.io.File;

/** A profiler that estimates the energy consumed by the current application. */
public final class Chappie implements Profiler<Iterable<FrequencyHistogram>> {
  @Component(modules = {CalmnessModule.class, PeriodicSamplingModule.class})
  interface ClerkFactory {
    Clerk<Iterable<FrequencyHistogram>> newClerk();
  }

  private static final ClerkFactory clerkFactory = DaggerChappie_ClerkFactory.builder().build();

  private Clerk<Iterable<FrequencyHistogram>> clerk;

  public Chappie() { }

  // starts a profiler if there is not one
  public void start() {
    if (clerk == null) {
      clerk = clerkFactory.newClerk();
      clerk.start();
    }
  }

  // stops the profiler if there is one
  public Iterable<FrequencyHistogram> stop() {
    Iterable<FrequencyHistogram> profiles = emptyList();
    if (clerk != null) {
      profiles = (Iterable<FrequencyHistogram>) clerk.stop();
      clerk = null;
    }
    return profiles;
  }

  public static void main(String[] args) throws Exception {
    String pid = args[0];
    File procPid = new File("/proc", args[0]);

    Chappie chappie = new Chappie();
    chappie.start();
    while (procPid.exists()) { }
    System.out.println(String.join(" ",
      "pid",
      pid,
      "consumed",
      String.format("%s", chappie.stop())));
  }
}
