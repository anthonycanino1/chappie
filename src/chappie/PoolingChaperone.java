/* ************************************************************************************************
 * Copyright 2017 SUNY Binghamton
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 * ***********************************************************************************************/

package chappie;

import java.util.Collection;

import java.util.Queue;
import java.util.LinkedList;

import java.util.Set;

import java.util.Map;
import java.util.HashMap;

import jrapl.EnergyCheckUtils;

public class PoolingChaperone extends Chaperone {
  private int count = 10;

  private long start;
  private Watcher[] watchers = new Watcher[1000];
  private Thread[] threads = new Thread[1000];
  private int[] starts = new int[1000];
  private Queue<Integer> free = new LinkedList<Integer>();
  //private Map<String, Integer> used = new HashMap<String, Integer>();
  private double[][] readings = new double[1000][];

  public PoolingChaperone() {
    start = System.currentTimeMillis();

    for(int i = 0; i < count; i++) {
      watchers[i] = new Watcher();
      watchers[i].assign();
      free.offer(i);
    }
  }

  public synchronized int assign() {
    while (free.isEmpty()) { }

    int next = free.poll();

    //used.put(Thread.currentThread().getName(), next);
    threads[next] = new Thread(watchers[next]);
    threads[next].setName("Chaperone-" + next);
    threads[next].start();
    starts[next] = (int)(System.currentTimeMillis() - start);
    readings[next] = EnergyCheckUtils.getEnergyStats();

    return next;
  }

  public synchronized double[] dismiss(int current) {
    //int current = used.get(Thread.currentThread().getName());

    threads[current].stop();
    try {
      threads[current].join();
    } catch (InterruptedException e) { }

    String name = Thread.currentThread().getName();

    double[] last_readings = EnergyCheckUtils.getEnergyStats();
    double usage = watchers[current].readUsage(0, watchers[current].getLast(), name);

    Map<Integer, Set<String>> results = watchers[current].read();
    for(int time : results.keySet())
      timeLine.put(time + starts[current], results.get(time));

    free.offer(current);

    for(int i = 0; i < last_readings.length; ++i)
      last_readings[i] = usage * (last_readings[i] - readings[current][i]);

    return last_readings;
  }
}
