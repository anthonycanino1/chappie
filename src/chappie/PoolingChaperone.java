package chappie;

import java.util.Queue;
import java.util.LinkedList;

import java.util.Set;

import java.util.Map;
import java.util.HashMap;

public class PoolingChaperone extends Chaperone {
  private int count = 10;

  private long start;
  private Watcher[] watchers = new Watcher[10];
  private Thread[] threads = new Thread[10];
  private long[] starts = new long[10];
  private Queue<Integer> free = new LinkedList<Integer>();
  private Map<String, Integer> used = new HashMap<String, Integer>();

  public PoolingChaperone() {
    start = System.currentTimeMillis();

    for(int i = 0; i < count; i++) {
      watchers[i] = new Watcher();
      watchers[i].assign();
      free.offer(i);
    }
  }

  public synchronized void assign() {
    while (free.isEmpty()) { }

    int next = free.poll();

    used.put(Thread.currentThread().getName(), next);
    threads[next] = new Thread(watchers[next]);
    threads[next].setName("Chaperone-" + next);
    threads[next].start();
    starts[next] = System.currentTimeMillis() - start;
  }

  public synchronized void dismiss() {
    int current = used.get(Thread.currentThread().getName());

    threads[current].stop();
    try {
      threads[current].join();
    } catch (InterruptedException e) { }

    Map<Long, Set<String>> results = watchers[current].read();
    for(long time : results.keySet())
      timeLine.put(time + starts[current], results.get(time));

    free.offer(current);
  }

  /*public void retire() {
    for(Watcher watcher : watcher)
      watcher.stop();

    for(Thread thread : watcherThreads)
      try {
        thread.join();
      } catch (InterruptedException e) { }

    timeLine.putAll(watcher.read());
    super.retire();
  }*/
}
