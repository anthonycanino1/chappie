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

package chappie.profile.sampling;

import chappie.profile.Record;
import java.util.ArrayList;

public final class ThreadSampler {
  private static final ThreadGroup rootGroup = getRootGroup();
  private static ThreadGroup getRootGroup() {
    ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
    ThreadGroup parentGroup;
    while ((parentGroup = rootGroup.getParent()) != null)
      rootGroup = parentGroup;
    return rootGroup;
  }

  public static Record sample() {
    // get all active threads (essentially everything but zombies)
    Thread[] threads = new Thread[rootGroup.activeCount()];
    while (rootGroup.enumerate(threads, true) == threads.length)
      threads = new Thread[threads.length * 2];

    // I'm only holding onto the thread's field values
    // so we don't mess with the GC
    ArrayList<ThreadRecord> records = new ArrayList<ThreadRecord>();
    for (Thread thread: threads)
      if (thread != null)
        records.add(new ThreadRecord(thread));

    return Record.of(records);
  }

  private static class ThreadRecord implements Record {
    private final long id;
    private final String name;
    private final Thread.State state;

    public ThreadRecord(Thread thread) {
      this.id = thread.getId();
      this.name = thread.getName();
      this.state = thread.getState();
    }

    @Override
    public String toString() {
      return "java id:" + id + ", thread name:" + name + ", thread state:" + state;
    }
  }
}
