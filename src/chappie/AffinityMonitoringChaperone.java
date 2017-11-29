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

import java.util.*;

import net.openhft.affinity.impl.*;

public class AffinityMonitoringChaperone extends Chaperone implements Runnable {
  private long polling = 10;

  private boolean running = true;

  public int assign() { running = true; return 0; }
  public double[] dismiss(int stamp) { running = false; return new double[0]; }

  public void run() {
    long start = System.currentTimeMillis();
    double[] previous = EnergyCheckUtils.getEnergyStats();

    while(running) {
      int stamp = (int)(System.currentTimeMillis() - start);

      Set<Thread> threadSet = Thread.getAllStackTraces().keySet();

      timeLine.put(stamp, new HashSet());

      for(Thread thread : threadSet) {
        if (thread.getState() == Thread.State.RUNNABLE) {
          String name = thread.getName();
          timeLine.get(stamp).add(name);

          if (!threadAffinities.containsKey(name))
            threadAffinities.put(name, new TreeMap<Integer, BitSet>());

          threadAffinities.get(name).put(stamp, LinuxJNAAffinity.INSTANCE.getAffinity());
        }
      }

      threadCount.put(stamp, new ArrayList<Integer>());
      threadCount.get(stamp).add(timeLine.get(stamp).size());
      threadCount.get(stamp).add(threadSet.size());

      try {
        Thread.sleep(polling);
      } catch (InterruptedException e) { Thread.currentThread().interrupt();}
    }

    return;
  }

  public void retire() {
    super.retire();
  }
}
