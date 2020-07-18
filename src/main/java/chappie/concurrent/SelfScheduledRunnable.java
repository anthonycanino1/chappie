package chappie.concurrent;

import static java.lang.Math.min;

/**
* A runnable that attempts to schedule an activity at a uniform millisecond
* rate. The thread will sleep until the interval is met if it is ahead of
* schedule. The thread will proceed to the next activity if it is behind
* schedule.
*/
// TODO(timur): it may be nice to have a log of the schedule
public final class SelfScheduledRunnable implements Runnable {
  /**
  * Given a start and interval, sleep for the amount of time until the end of
  * the next interval.
  */
  private static void sleepUntil(long start, long interval) throws InterruptedException {
    long elapsed = System.nanoTime() - start;
    long millis = elapsed / 1000000;
    int nanos = (int)(elapsed - millis * 1000000);

    millis = interval - millis - (nanos > 0 ? 1 : 0);
    nanos = min(1000000 - nanos, 999999);

    if (millis >= 0 && nanos > 0) {
      Thread.sleep(millis, nanos);
    } else if (Thread.currentThread().isInterrupted()) {
      throw new InterruptedException();
    }
  }

  private final long rate;
  private final Runnable runnable;
  // private final Schedule schedule = new Schedule();

  // TODO(timur): explore non-runnable interfaces
  public SelfScheduledRunnable(Runnable runnable, long rate) {
    this.runnable = runnable;
    this.rate = rate;
  }

  /**
  * Execute the underlying action, then sleep until the next interval.
  * Interrupting the executing thread will terminate the runnable.
  *
  * NOTE: the self-scheduling will terminate if there is an exception in the
  * runnable.
  */
  @Override
  public void run() {
    boolean terminated = false;
    while (!terminated) {
      long start = System.nanoTime();
      runnable.run();
      long duration = System.nanoTime();
      try {
        sleepUntil(start, rate);
      } catch (InterruptedException e) {
        terminated = true;
      }
      long end = System.nanoTime();
      // schedule.add(new ScheduleEntry(start, duration, end));
    }
  }
}
