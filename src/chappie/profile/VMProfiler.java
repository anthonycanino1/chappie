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

package chappie.profile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

import chappie.util.*;

public class VMProfiler extends Profiler {
  ThreadGroup rootGroup;
  public VMProfiler(int rate, int time) {
    super(rate, time);

    rootGroup = Thread.currentThread().getThreadGroup();
    ThreadGroup parentGroup;
    while ((parentGroup = rootGroup.getParent()) != null)
      rootGroup = parentGroup;
  }

  private class ThreadRecord implements Record {
    private Integer epoch;
    private long id;
    private Thread.State state;

    private ThreadRecord(int epoch, long id, Thread.State state) {
      this.epoch = epoch;
      this.id = id;
      this.state = state;
    }

    @Override
    public String toString() {
      return Arrays.stream(new Object[]{epoch, id, state})
        .map(Object::toString)
        .collect(Collectors.joining(";"));
    }
  }

  HashMap<Object, Object> mapping = new HashMap<Object, Object>();
  protected void sampleImpl(int epoch) {
    // get all active threads (essentially everything but zombies)
    Thread[] threads = new Thread[rootGroup.activeCount()];
    while (rootGroup.enumerate(threads, true) == threads.length)
      threads = new Thread[threads.length * 2];

    // I'm only holding onto the thread's field values
    // so we don't mess with the GC
    for (Thread thread: threads)
      if (thread != null) {
        if (!mapping.containsKey(thread.getId()))
          mapping.put(thread.getId(), thread.getName());
        data.add(new ThreadRecord(epoch, thread.getId(), thread.getState()));
      }
  }

  private static String[] header = new String[] { "epoch", "id", "state" };
  public void dump() throws IOException {
    logger.info("writing vm data");

    chappie.util.CSV.write(data, "data/vm.csv", header);
    chappie.util.JSON.write(mapping, "data/id.json");
  }
}
