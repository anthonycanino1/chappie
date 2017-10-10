package chappie;

import java.util.Queue;
import java.util.LinkedList;

import java.util.Set;

import java.util.Map;
import java.util.HashMap;

import jrapl.EnergyCheckUtils;

public class PoolingChaperone extends Chaperone {
  private int count = 10;

  private long start;
  private Watcher[] watchers = new Watcher[10];
  private Thread[] threads = new Thread[10];
  private long[] starts = new long[10];
  private Queue<Integer> free = new LinkedList<Integer>();
  private Map<String, Integer> used = new HashMap<String, Integer>();
  private double[][] readings = new double[10][];

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
    readings[next] = EnergyCheckUtils.getEnergyStats();
  }

  public synchronized double[] dismiss() {
    int current = used.get(Thread.currentThread().getName());

    threads[current].stop();
    try {
      threads[current].join();
    } catch (InterruptedException e) { }

    double[] last_readings = EnergyCheckUtils.getEnergyStats();
    Map<Long, Set<String>> results = watchers[current].read();

    int count = 0;
    for(Set<String> stamp: results.values())
      for(String thread: stamp)
        if(thread == Thread.currentThread().getName())
          count++;

    double usage = count / (double)results.size();

    for(long time : results.keySet())
      timeLine.put(time + starts[current], results.get(time));

    free.offer(current);

    for(int i = 0; i < last_readings.length; ++i)
      last_readings[i] = usage * (last_readings[i] - readings[current][i]);

    return last_readings;
  }
}
