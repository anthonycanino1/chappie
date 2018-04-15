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

  private int pid = -1;
  private Thread thread;
  private com.sun.management.ThreadMXBean bean;

  public GlobalChaperone() {
    pid = GLIBC.getProcessId();
    bean = (com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();

    thread = new Thread(this, "Chaperone");
    thread.start();
  }

  public GlobalChaperone(int polling) {
    this.polling = polling;
    pid = GLIBC.getProcessId();
    bean = (com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();

    thread = new Thread(this, "Chaperone");
    thread.start();
  }

  private int curr = 0;

  private long polling = 5000;
  private boolean running = false;

  public int assign() { running = true; return 0; }
  public List<Double> dismiss(int stamp) { running = false; return null; }

  private Map<String, Long> lastMemory = new HashMap<String, Long>();

  public void run() {
    int counter = 0;
    while(!running) {}

    double[] previous = jrapl.EnergyCheckUtils.getEnergyStats();

    while(running) {
      Set<Thread> threadSet = Thread.getAllStackTraces().keySet();

      activity.put(curr, new ArrayList<Set<String>>());
      activity.get(curr).add(new HashSet<String>());
      activity.get(curr).add(new HashSet<String>());

      int i = 0;

      for(Thread thread : threadSet) {
        String name = thread.getName();
        if (thread.getState() == Thread.State.RUNNABLE)
          activity.get(curr).get(0).add(name);
        else
          activity.get(curr).get(1).add(name);

        // if (!cores.containsKey(name))
        //   cores.put(name, new TreeMap<Integer, Integer>());
        // if(Chaperone.threadMap.containsKey(name))
        //   try{
        //     cores.get(name).put(curr, GLIBC.getCore(pid, Chaperone.threadMap.get(name)));
        //   } catch(ArrayIndexOutOfBoundsException e) { }

        if (!memory.containsKey(name)) {
          memory.put(name, new TreeMap<Integer, Long>());
        }

        long used = bean.getThreadAllocatedBytes(Thread.currentThread().getId());
        memory.get(name).put(curr, used);

        if(!power.containsKey(name)) {
          power.put(name, new TreeMap<Integer, List<Double>>());
          power.get(name).put(curr - (int)polling, Arrays.asList(0.0,0.0,0.0));
        }
      }

      try {
        double[] next = jrapl.EnergyCheckUtils.getEnergyStats();
        double[] current = new double[next.length];
        int size = activity.get(curr).get(0).size();
        for (int n = 0; n < current.length; ++n)
          current[n] = (next[n] - previous[n]) / size;
        previous = next;

        int stump = curr - (int)polling;

        for(String name: activity.get(curr).get(0)) {
          List<Double> reading = new ArrayList<Double>();
          List<Double> last = power.get(name).get(stump);
          for(int n = 0; n < current.length; ++n)
            reading.add(current[n] + last.get(n));

          power.get(name).put(curr, reading);
        }

        for(String name: activity.get(curr).get(1)) {
          if(curr == 0)
            power.get(name).put(curr, Arrays.asList(0.0,0.0,0.0));
          else
            power.get(name).put(curr, power.get(name).get(stump));
        }
      } catch(Exception e) {
        System.out.println("whoops");
      }

      curr += polling;

      //counter++;

      // long waitUntil = System.nanoTime() + polling * 1000;
      // while(waitUntil > System.nanoTime());

      try {
        Thread.sleep(polling);
      } catch (InterruptedException e) { Thread.currentThread().interrupt();}
    }

    return;
  }

  public void retire() {
    running = false;
    try {
      thread.join();
    } catch (InterruptedException e) { }

    super.retire();
  }
}
