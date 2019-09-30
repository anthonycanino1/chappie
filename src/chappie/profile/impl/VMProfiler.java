/* ************************************************************************************************
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * Copyright 2019 SUNY Binghamton
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

package chappie.profile.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

import chappie.Chaperone;
import chappie.profile.*;
import chappie.profile.util.*;

public class VMProfiler extends Profiler {
  ThreadGroup rootGroup;

  public VMProfiler(int rate, int time, String workDirectory) {
    super(rate, time, workDirectory);

    rootGroup = Thread.currentThread().getThreadGroup();
    ThreadGroup parentGroup;
    while ((parentGroup = rootGroup.getParent()) != null)
      rootGroup = parentGroup;
  }

  private static class ThreadRecord extends Record {
    private long id;
    private Thread.State state;

    public ThreadRecord(int epoch, Thread thread) {
      this.epoch = epoch;
      this.id = thread.getId();
      this.state = thread.getState();
    }

    public String stringImpl() {
      return Long.toString(id) + ";" + state.toString();
    }

    private static String[] header = new String[] { "epoch", "id", "state" };
    public static String[] getHeader() {
      return header;
    };
  }

  HashMap<Integer, String> names = new HashMap<Integer, String>();
  protected void sampleImpl(int epoch) {
    // get all active threads (essentially everything but zombies)
    Thread[] threads = new Thread[rootGroup.activeCount()];
    while (rootGroup.enumerate(threads, true) == threads.length)
      threads = new Thread[threads.length * 2];

    // I'm only holding onto the thread's field values
    // so we don't mess with the GC
    for (Thread thread: threads)
      if (thread != null) {
        if (!names.containsKey(thread.getId()))
          names.put((int)thread.getId(), thread.getName());
        data.add(new ThreadRecord(epoch, thread));
      }
  }

  public void dumpImpl() throws IOException {
    CSV.write(data, ThreadRecord.getHeader(), Chaperone.getWorkDirectory() + "/vm.csv");
    JSON.write(names, Chaperone.getWorkDirectory() + "/id.json");
  }
}
