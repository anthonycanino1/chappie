package chappie.util.concurrent;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

/**
* An executor whose submissions are managed like a collection.
* Extremely unsafe at the moment.
*/
// TODO(timur): i really need to make this safer; it's too easy for even me to break
public final class RunnableCollectionExecutor {
  private final ArrayList<Runnable> runnables = new ArrayList<>();

  private boolean isRunning = false;
  private ExecutorService executor;
  private ThreadFactory threadFactory;

  public RunnableCollectionExecutor() { }

  public void setThreadFactory(ThreadFactory threadFactory) {
    this.threadFactory = threadFactory;
  }

  public void add(Runnable runnable) {
    runnables.add(runnable);
  }

  /** Starts an internal executor to execute all the current runnables. */
  public synchronized void start() {
    if (!isRunning) {
      isRunning = true;
      if (threadFactory != null) {
        executor = Executors.newFixedThreadPool(runnables.size() + 1, threadFactory);
      } else {
        executor = Executors.newFixedThreadPool(runnables.size() + 1);
      }

      for (Runnable runnable: runnables) {
        executor.submit(runnable);
      }
    }
  }

  /** Stops all threads created by the internal executor. */
  public synchronized void stop() {
    if (isRunning) {
      try {
        executor.shutdownNow();
        while (!executor.awaitTermination(250, MILLISECONDS)) { }
      } catch (Exception e) {
        e.printStackTrace();
      }
      executor = null;
      isRunning = false;
    }
  }
}
