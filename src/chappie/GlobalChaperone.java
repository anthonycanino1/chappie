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

import chappie.util.GLIBC;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

import java.util.Set;
import java.util.HashSet;

import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;

import java.io.*;

import java.lang.management.*;
import com.sun.management.*;

import jrapl.EnergyCheckUtils.*;

public class GlobalChaperone extends Chaperone {

  private Set<String> systemThreads = new HashSet<String>();

  private int pid = -1;
  private Thread thread;
  private com.sun.management.ThreadMXBean bean;

  private int polling = 2;
  public GlobalChaperone(Integer polling) {
    this.polling = polling;
    pid = GLIBC.getProcessId();
    bean = (com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();

    systemThreads.add("main");
    systemThreads.add("Common-Cleaner");
    systemThreads.add("Finalizer");
    systemThreads.add("Reference Handler");
    systemThreads.add("Signal Dispatcher");
    systemThreads.add("process reaper");
    systemThreads.add("Chaperone");
    systemThreads.add("DestroyJavaVM");

    thread = new Thread(this, "Chaperone");
    thread.start();
  }

  public void run() {
    int curr = 0;
    double[] previous = jrapl.EnergyCheckUtils.getEnergyStats();

    while(!thread.isInterrupted()) {
      Set<String> threadNames = new HashSet<String>();
      Set<Thread> threadSet = Thread.getAllStackTraces().keySet();

      activity.put(curr, new ArrayList<Set<String>>());
      activity.get(curr).add(new HashSet<String>());
      activity.get(curr).add(new HashSet<String>());

      for(Thread thread : threadSet) {
        String name = thread.getName();
        threadNames.add(name);

        if (thread.getState() == Thread.State.RUNNABLE)
          activity.get(curr).get(0).add(name);
        else
          activity.get(curr).get(1).add(name);

        if (!cores.containsKey(name))
          cores.put(name, new TreeMap<Integer, Integer>());
        if(curr % (polling * 10) == 0) {
          if (Thread.tidMap.containsKey(name)) {
            cores.get(name).put(curr, GLIBC.getCore(pid, Thread.tidMap.get(name)));
          } else {
            cores.get(name).put(curr, -1);
          }
        } else {
          cores.get(name).put(curr, cores.get(name).get(curr - polling));
        }

        long used = bean.getThreadAllocatedBytes(Thread.currentThread().getId());
        if (!bytes.containsKey(name))
          bytes.put(name, new TreeMap<Integer, Long>());

        bytes.get(name).put(curr, used);
      }

      double[] next = jrapl.EnergyCheckUtils.getEnergyStats();
      double[] current = new double[next.length];
      int size = activity.get(curr).get(0).size();
      for (int n = 0; n < current.length; ++n)
        current[n] = (next[n] - previous[n]) / size;
      previous = next;

      for(String name: activity.get(curr).get(0)) {
        List<Double> reading = new ArrayList<Double>();
        for(int n = 0; n < current.length; ++n)
          reading.add(current[n]);

        if(!power.containsKey(name))
          power.put(name, new TreeMap<Integer, List<Double>>());

        power.get(name).put(curr, reading);
      }

      for(String name: activity.get(curr).get(1)) {
        if(!power.containsKey(name))
          power.put(name, new TreeMap<Integer, List<Double>>());

        power.get(name).put(curr, Arrays.asList(0.0,0.0,0.0));
      }

      threadNames.removeAll(systemThreads);
      if(threadNames.size() == 0){
        Thread.currentThread().interrupt();
      }

      curr += polling;

      try {
        Thread.sleep(polling);
      } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    retire();
    return;
  }
}
