package chappie;

import java.util.Set;
import java.util.HashSet;

import java.util.Map;
import java.util.TreeMap;

public class Watcher implements Runnable {
  private long polling = 10;
  public long getPolling() { return polling; }
  public void setPolling(long time) { polling = time; }

  private boolean running = true;
  public void stop() { running = false; }

  private boolean assigned = false;
  public void assign() { assigned = true; }
  public void dismiss() { assigned = false; }

  private long duration = 0;
  public long getDuration() { return duration; }

  private Map<Long, Set<String>> timeLine = new TreeMap<Long, Set<String>>();

  public void run() {
    long start = System.currentTimeMillis();
    while(running) {
      if(assigned) {
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();

        long time = System.currentTimeMillis() - start;
        duration = time;
        if(threadSet.size() > 0)
          timeLine.put(time, new HashSet());

        for(Thread thread : threadSet) {
          if (thread.getState() == Thread.State.RUNNABLE)
            timeLine.get(time).add(thread.getName());
        }
      }
      try {
        Thread.sleep(polling);
      } catch (InterruptedException e) { Thread.currentThread().interrupt();}
    }

    return;
  }

  public Map<Long, Set<String>> read() {
    return timeLine;
  }
}
