/* ************************************************************************************************
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * Copyright 2017 SUNY Binghamton
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

import chappie.util.*;

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

import java.lang.Thread;
import java.lang.reflect.Method;

import java.net.URL;
import java.net.URLClassLoader;

public class Chaperone implements Runnable {

  // private Set<String> systemThreads = new HashSet<String>();

  private Thread thread;
  private com.sun.management.ThreadMXBean bean;

  private int polling = 2;
  public Chaperone() {
    setup();
  }

  public Chaperone(Integer polling) {
    this.polling = polling;
    setup();
  }

  private void setup() {
    GLIBC.getProcessId();
    // pid = GLIBC.getProcessId();
    bean = (com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
    //
    // systemThreads.add("main");
    // systemThreads.add("Common-Cleaner");
    // systemThreads.add("Finalizer");
    // systemThreads.add("Reference Handler");
    // systemThreads.add("Signal Dispatcher");
    // systemThreads.add("process reaper");
    // systemThreads.add("Chaperone");
    // systemThreads.add("DestroyJavaVM");

    thread = new Thread(this, "Chaperone");
    thread.start();
  }

  protected Map<Integer, List<Set<String>>> activity = new TreeMap<Integer, List<Set<String>>>();
  protected Map<String, Map<Integer, List<Double>>> power = new HashMap<String, Map<Integer, List<Double>>>();
  protected Map<String, Map<Integer, Integer>> cores = new HashMap<String, Map<Integer, Integer>>();
  protected Map<String, Map<Integer, Long>> bytes = new HashMap<String, Map<Integer, Long>>();

  public void run() {

    int curr = 0;
    double[] previous = jrapl.EnergyCheckUtils.getEnergyStats();

    while(!thread.isInterrupted()) {

      // Set<String> threadNames = new HashSet<String>();
      Set<Thread> threads = Thread.getAllStackTraces().keySet();

      activity.put(curr, new ArrayList<Set<String>>());
      activity.get(curr).add(new HashSet<String>());
      activity.get(curr).add(new HashSet<String>());


      for(Thread thread: threads) {

        String name = thread.getName();
        // threadNames.add(name);

        if (thread.getState() == Thread.State.RUNNABLE)
          activity.get(curr).get(0).add(name);
        else
          activity.get(curr).get(1).add(name);

        if (!cores.containsKey(name))
          cores.put(name, new TreeMap<Integer, Integer>());
        if(curr % (polling * 10) == 0) {
          // if (GLIBC.tidMap.containsKey(name)) {
            // cores.get(name).put(curr, GLIBC.getCore(pid, Thread.tidMap.get(name)));
          cores.get(name).put(curr, GLIBC.getCore(name));
        } else {
          cores.get(name).put(curr, cores.get(name).get(curr - polling));
        }

        long used = bean.getThreadAllocatedBytes(Thread.currentThread().getId());

        if (!bytes.containsKey(name)) {
          bytes.put(name, new TreeMap<Integer, Long>());
          bytes.get(name).put(curr - polling, 0L);
        }

        long last = bytes.get(name).get(curr - polling);
        bytes.get(name).put(curr, used - last);
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

      // threadNames.removeAll(systemThreads);
      // if(threadNames.size() == 0){
      //   Thread.currentThread().interrupt();
      // }

      curr += polling;

      try {
        Thread.sleep(polling);
      } catch (InterruptedException e) { Thread.currentThread().interrupt();}
    }

    retire();
    return;
  }

  public void dismiss() { thread.interrupt(); }

  private void retire() {
    PrintWriter log = null;

    String path = System.getenv("CHAPPIE_TRACE_LOG");
    if (path == null)
      path = "chappie.trace.csv";

    try {
      log = new PrintWriter(new BufferedWriter(new FileWriter(path)));
    } catch (IOException io) {
      System.err.println("Error: " + io.getMessage());
      throw new RuntimeException("uh oh");
    }

    String message = "time,count,state\n";
    log.write(message);
    for (Integer time : activity.keySet()) {
      message = time + "," + activity.get(time).get(0).size() + "," + "active\n";
      message += time + "," + activity.get(time).get(1).size() + "," + "inactive\n";
      log.write(message);
    }
    log.close();

    path = System.getenv("CHAPPIE_THREAD_LOG");
    if (path == null)
      path = "chappie.thread.csv";

    try {
      log = new PrintWriter(new BufferedWriter(new FileWriter(path)));
    } catch (IOException io) {
      System.err.println("Error: " + io.getMessage());
      throw new RuntimeException("uh oh");
    }

    message = "time,thread,core,package,dram,bytes\n";
    log.write(message);
    for (String name : power.keySet())
      for (Integer time : power.get(name).keySet()) {
        message = time + "," + name + "," + cores.get(name).get(time);
        message += "," + power.get(name).get(time).get(2) + "," + power.get(name).get(time).get(0);
        message += "," + bytes.get(name).get(time) + "\n";
        log.write(message);
      }

    log.close();
  }

  public static void main(String[] args) throws IOException {
    URLClassLoader loader;
    try {
      System.out.println("Loading " + args[0]);
      loader = new URLClassLoader(new URL[] {new URL(args[0])});
      Method main = loader.loadClass(args[1]).getMethod("main", String[].class);

      System.setSecurityManager(new ExitStopper());

      Chaperone chaperone = null;
      try {
        List<String> params = new ArrayList<String>();
        for (int i = 2; i < args.length; ++i) {
          String[] temp_params = args[i].split(" ", 100);
          for (int k = 0; k < temp_params.length; ++k)
            params.add(temp_params[k]);
        }

        System.out.println("Running " + args[1] + ".main");
        System.out.println("==================================================");

        chaperone = new Chaperone();
        main.invoke(null, (Object)params.toArray(new String[params.size()]));

        System.out.println("==================================================");
        System.out.println("Dismissing the chaperone");
        chaperone.dismiss();
      } catch(Exception e) {
        System.out.println("==================================================");
        System.out.println("Dismissing the chaperone");
        chaperone.dismiss();
      }
    } catch(Exception e) {
      System.out.println("Unable to load " + args[0]);
    }
  }
}
