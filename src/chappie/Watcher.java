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

public class Watcher implements Runnable {
  private long polling = 5;
  public void setPolling(long time) { polling = time; }

  private boolean running = true;
  public void stop() { running = false; }

  private int last = 0;
  public int getLast() { return last; }

  private boolean assigned = false;
  public void assign() { assigned = true; }
  public void dismiss() { assigned = false; }

  private List<Set<String>> timeLine = new ArrayList<Set<String>>();
  private List<Integer> threadCount = new ArrayList<Integer>();

  public void run() {
    long start = System.currentTimeMillis();
    while(running) {
      if(assigned) {
        synchronized(timeLine) {
          Set<Thread> threadSet = Thread.getAllStackTraces().keySet();

          last = timeLine.size();
          timeLine.add(new HashSet());
          threadCount.add(threadSet.size());

          for(Thread thread : threadSet) {
            if (thread.getState() == Thread.State.RUNNABLE)
              timeLine.get(last).add(thread.getName());
          }
        }
      }
      try {
        Thread.sleep(polling);
      } catch (InterruptedException e) { Thread.currentThread().interrupt();}
    }

    return;
  }

  public Map<Integer, Set<String>> read() {
    Map<Integer, Set<String>> mappedTimeLine = new TreeMap<Integer, Set<String>>();
    int i = 0;
    for(Set<String> threads: timeLine)
      mappedTimeLine.put(i++ * (int)polling, threads);

    return mappedTimeLine;
  }

  public Map<Integer, List<Integer>> readCount() {
    Map<Integer, List<Integer>> mappedActive = new TreeMap<Integer, List<Integer>>();
    for(int n = 0; n < timeLine.size(); ++n) {
      int stamp = n * (int)polling;
      mappedActive.put(stamp, new ArrayList());
      mappedActive.get(stamp).add(timeLine.get(n).size());
      mappedActive.get(stamp).add(threadCount.get(n));
    }

    return mappedActive;
  }

  public double readUsage(int start, int end, String name) {
    int count = 0;
    synchronized(timeLine) {
      for(int i = start; i <= end; ++i)
        if(timeLine.get(i).contains(name))
          count++;
    }

    return count / (double)(end - start + 1);
  }
}
