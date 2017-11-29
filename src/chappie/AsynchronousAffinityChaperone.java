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

import java.util.List;
import java.util.ArrayList;

import java.util.Set;
import java.util.BitSet;

import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;

import vanilla.java.affinity.impl.*;

public class AsynchronousAffinityChaperone extends Chaperone {

  private int assigned = 0;

  private Thread watcherThread;
  private Watcher watcher = new Watcher();

  private int mode;

  public AsynchronousAffinityChaperone() {
    watcherThread = new Thread(watcher);
    watcherThread.setName("Chaperone");
    watcherThread.start();

    mode = Integer.parseInt(System.getenv("CHAPPIE"));
  }

  public synchronized int assign() {
    if (mode > 0) {
      if (++assigned > 0)
        watcher.assign();

      String name = Thread.currentThread().getName();
      int stamp = watcher.getLast();

      if (!threadAffinities.containsKey(name))
        threadAffinities.put(name, new TreeMap<Integer, Long>());

      threadAffinities.get(name).put(stamp, PosixJNAAffinity.INSTANCE.getAffinity());

      return stamp;
    } else
      return 0;
  }

  public synchronized double[] dismiss(int stamp1) {
    if (mode > 0) {
      if (--assigned <= 0)
        watcher.dismiss();

      String name = Thread.currentThread().getName();

      int stamp2 = watcher.getLast();
      double usage = watcher.readUsage(stamp1, stamp2, Thread.currentThread().getName());

      return new double[] {usage};
    } else
      return new double[] {0};
  }

  public void retire() {
    watcher.stop();
    try {
      watcherThread.join();
    } catch (InterruptedException e) { }

    timeLine.putAll(watcher.read());
    threadCount.putAll(watcher.readCount());
    super.retire();
  }
}
