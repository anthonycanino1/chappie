package chappie;

import static java.util.Collections.emptyList;

import clerk.Clerk;
import clerk.Profiler;
import clerk.concurrent.PeriodicSamplingModule;
import dagger.Component;
import java.io.File;

/** A profiler that estimates the energy consumed by the current application. */
public final class CalmnessProfiler implements Profiler<Iterable<long[]>> {
  @Component(modules = {CalmnessModule.class, PeriodicSamplingModule.class})
  interface ClerkFactory {
    Clerk<Iterable<long[]>> newClerk();
  }

  private static final ClerkFactory clerkFactory = DaggerCalmnessProfiler_ClerkFactory.builder().build();

  private Clerk<Iterable<long[]>> clerk;

  public CalmnessProfiler() { }

  // starts a profiler if there is not one
  public void start() {
    if (clerk == null) {
      clerk = clerkFactory.newClerk();
      clerk.start();
    }
  }

  // stops the profiler if there is one
  public Iterable<long[]> stop() {
    Iterable<long[]> profiles = emptyList();
    if (clerk != null) {
      profiles = (Iterable<long[]>) clerk.stop();
      clerk = null;
    }
    return profiles;
  }

  public static void main(String[] args) throws Exception {
    String pid = args[0];
    File procPid = new File("/proc", args[0]);

    CalmnessProfiler chappie = new CalmnessProfiler();
    chappie.start();
    while (procPid.exists()) { }
    for (long[] hist: chappie.stop()) {
      for (long val: hist) {
        System.out.print(val + " ");
      }
      System.out.println();
    }


    // }
    // System.out.println(String.join(" ",
    //   "pid",
    //   pid,
    //   "consumed",
    //   String.format("%s", chappie.stop())));
  }
}
