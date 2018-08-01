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

import java.nio.file.Paths;
import java.nio.file.Files;

import java.lang.management.*;
import com.sun.management.*;

import jrapl.EnergyCheckUtils.*;

import java.lang.Thread;
import java.lang.reflect.Method;

import java.net.URL;
import java.net.URLClassLoader;

public class Chaperone implements Runnable {

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
    bean = (com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();

    thread = new Thread(this, "Chaperone");
    thread.start();
  }

  // protected Map<String, Map<Integer, ThreadState>> state = new HashMap<String, Map<Integer, ThreadState>>();

  protected List<List<Object>> threads = new ArrayList<List<Object>>();
  protected List<List<Object>> energy = new ArrayList<List<Object>>();

  // protected List<String> state = new ArrayList<String>();
  //
  // protected Map<Integer, List<Set<String>>> activity = new TreeMap<Integer, List<Set<String>>>();
  // protected Map<String, Map<Integer, List<Double>>> power = new HashMap<String, Map<Integer, List<Double>>>();
  // protected Map<String, Map<Integer, Integer>> cores = new HashMap<String, Map<Integer, Integer>>();
  // protected Map<String, Map<Integer, Long>> bytes = new HashMap<String, Map<Integer, Long>>();

  public void run() {
    int curr = 0;
    List<Object> measure;

    // double[] current = jrapl.EnergyCheckUtils.getEnergyStats();
    // double[] start = previous;
    // int sockets = previous.length / 3;

    // thread.add("time,thread,state,core,bytes");

    // energy.add("time,package,dram");

    while(!thread.isInterrupted()) {
      // List<String> frame = new HashMap<String, List<String>>();
      Set<Thread> threadSet = Thread.getAllStackTraces().keySet();

      // activity.put(curr, new ArrayList<Set<String>>());
      // for (int i = 0; i < sockets + 2; ++i)
      //   activity.get(curr).add(new HashSet<String>());

      for(Thread thread: threadSet) {
        measure = new ArrayList<Object>();

        measure.add(curr);

        String name = thread.getName();
        measure.add(name);

        if (thread.getState() == Thread.State.RUNNABLE)
          measure.add(true);
        else
          measure.add(false);

        measure.add(GLIBC.getCore(name));

        measure.add(bean.getThreadAllocatedBytes(Thread.currentThread().getId()));

        threads.add(measure);
        // if (!cores.containsKey(name)) {
        //   cores.put(name, new HashMap<Integer, Integer>());
        //   cores.get(name).put(curr - polling, -1);
        // }
        // if(curr % (polling * 1) == 0)
        //   cores.get(name).put(curr, GLIBC.getCore(name));
        // else
        //   cores.get(name).put(curr, cores.get(name).get(curr - polling));
        // frame.get(name).add(GLIBC.getCore(name).toString());

        // try {
        //   if (thread.getState() != Thread.State.RUNNABLE)
        //     activity.get(curr).get(0).add(name);
        //   else if (cores.get(name).get(curr) == -1)
        //     activity.get(curr).get(1).add(name);
        //   else if (cores.get(name).get(curr) < 20)
        //     activity.get(curr).get(2).add(name);
        //   else if (cores.get(name).get(curr) < 40)
        //     activity.get(curr).get(3).add(name);
        // } catch(Exception e) { }

        // long used = bean.getThreadAllocatedBytes(Thread.currentThread().getId());
        // frame.get(name).add(used.toString());

        // if (!bytes.containsKey(name)) {
        //   bytes.put(name, new HashMap<Integer, Long>());
        //   bytes.get(name).put(curr - polling, 0L);
        // }
        //
        // try {
        //   long last = bytes.get(name).get(curr - polling);
        //   bytes.get(name).put(curr, used - last);
        // } catch(Exception e) {
        // }
      }

      double[] reading = jrapl.EnergyCheckUtils.getEnergyStats();

      for (int i = 0; i < reading.length / 3; ++i) {
        measure = new ArrayList<Object>();
        measure.add(curr);
        measure.add(i + 1);
        measure.add(reading[3 * i + 2]);
        measure.add(reading[3 * i]);
        energy.add(measure);
      }
      // double[] current = new double[next.length];
      //
      // // Threads with core readings
      // for (int i = 0; i < sockets; ++i) {
      //   double size = activity.get(curr).get(i + 2).size() + (double)(activity.get(curr).get(1).size()) / 2.0;
      //
      //   for (int n = 3 * i; n < 3 * i + 3; ++n) {
      //     double measurement = next[n] - previous[n];
      //     if (measurement < 0)
      //       measurement += jrapl.EnergyCheckUtils.wraparoundValue;
      //     current[n] = measurement / size;
      //   }
      //
      //   for(String name: activity.get(curr).get(i + 2)) {
      //     List<Double> reading = new ArrayList<Double>();
      //     for (int n = 3 * i; n < 3 * i + 3; ++n)
      //     reading.add(current[n]);
      //
      //
      //     if(!power.containsKey(name))
      //       power.put(name, new TreeMap<Integer, List<Double>>());
      //
      //     // frame.get(name).add(reading.toString());
      //     power.get(name).put(curr, reading);
      //   }
      // }
      //
      // // Threads with no core reading
      // for(String name: activity.get(curr).get(1)) {
      //   List<Double> reading = new ArrayList<Double>();
      //   for (int n = 0; n < 3; ++n)
      //     reading.add((current[n] + current[n + 3]) / 2.0);
      //
      //   if(!power.containsKey(name))
      //     power.put(name, new HashMap<Integer, List<Double>>());
      //
      //   // frame.get(name).add(reading.toString());
      //   power.get(name).put(curr, reading);
      // }
      //
      // // Inactive Threads
      // for(String name: activity.get(curr).get(1)) {
      //   if(!power.containsKey(name))
      //     power.put(name, new HashMap<Integer, List<Double>>());
      //
      //   // frame.get(name).add(Arrays.asList(0.0,0.0,0.0));
      //   power.get(name).put(curr, Arrays.asList(0.0,0.0,0.0));
      // }
      //
      // previous = next;

      curr += polling;

      try {
        Thread.sleep(polling);
      } catch (InterruptedException e) { Thread.currentThread().interrupt();}
    }

    retire();
    return;
  }

  public void dismiss() {
    thread.interrupt();
    try {
      thread.join();
    } catch (Exception e) { }
  }

  private void retire() {
    PrintWriter log = null;

    String path = System.getenv("CHAPPIE_TRACE_LOG");
    if (path == null)
      path = "chappie.trace.csv";

    try {
      log = new PrintWriter(new BufferedWriter(new FileWriter(path)));
    } catch (Exception io) {
      System.err.println("Error: " + io.getMessage());
    }

    String message = "time,socket,package,dram,\n";
    log.write(message);
    for (List<Object> frame : energy) {
      message = "";
      for (Object o: frame)
        message += o.toString() + ",";
      message += "\n";
      log.write(message);
    }
    log.close();

    path = System.getenv("CHAPPIE_THREAD_LOG");
    if (path == null)
      path = "chappie.thread.csv";

    try {
      log = new PrintWriter(new BufferedWriter(new FileWriter(path)));
    } catch (Exception io) {
      System.err.println("Error: " + io.getMessage());
    }

    message = "time,thread,state,core,bytes,\n";
    log.write(message);
    for (List<Object> frame : threads) {
      message = "";
      for (Object o: frame)
        message += o.toString() + ",";
      message += "\n";
      log.write(message);
    }
    log.close();

    // String message = "time,count,state\n";
    // log.write(message);
    // for (Integer time : activity.keySet()) {
    //   message = time + "," + activity.get(time).get(0).size() + "," + "inactive\n";
    //   message += time + "," + activity.get(time).get(1).size() + "," + "no socket\n";
    //   for (int i = 2; i < activity.get(time).size(); ++i)
    //     message += time + "," + activity.get(time).get(i).size() + "," + "socket " + (i - 1) + "\n";
    //   log.write(message);
    // }
    // log.close();
    //
    // path = System.getenv("CHAPPIE_THREAD_LOG");
    // if (path == null)
    //   path = "chappie.thread.csv";
    //
    // try {
    //   log = new PrintWriter(new BufferedWriter(new FileWriter(path)));
    // } catch (IOException io) {
    //   System.err.println("Error: " + io.getMessage());
    //   throw new RuntimeException("uh oh");
    // }
    //
    // message = "time,thread,core,package,dram,bytes\n";
    // log.write(message);
    // for (String name : power.keySet())
    //   for (Integer time : power.get(name).keySet()) {
    //     message = time + "," + name + "," + cores.get(name).get(time);
    //     message += "," + power.get(name).get(time).get(2) + "," + power.get(name).get(time).get(0);
    //     message += "," + bytes.get(name).get(time) + "\n";
    //     log.write(message);
    //   }
    //
    // log.close();
  }

  public static void main(String[] args) throws IOException {
    Integer iterations = 10;
    try {
      iterations = Integer.parseInt(System.getenv("ITERS"));
    } catch(Exception e) { }

    URLClassLoader loader;
    try {
      System.out.println("Loading " + args[0]);
      loader = new URLClassLoader(new URL[] {new URL(args[0])});
      Method main = loader.loadClass(args[1]).getMethod("main", String[].class);

      try {
        List<String> params = new ArrayList<String>();
        for (int i = 2; i < args.length; ++i) {
          String[] temp_params = args[i].split(" ", 100);
          for (int k = 0; k < temp_params.length; ++k)
            params.add(temp_params[k]);
        }

        for(int i = 0; i < iterations; ++i) {
          System.out.println("Iteration " + (i + 1));
          System.out.println("Running " + args[1] + ".main");
          System.out.println("Arguments: " + params.toString());
          System.out.println("==================================================");

          Chaperone chaperone = new Chaperone();
          main.invoke(null, (Object)params.toArray(new String[params.size()]));
          System.out.println("==================================================");
          System.out.println("Dismissing the chaperone");
          chaperone.dismiss();
          Files.move(Paths.get("chappie.trace.csv"), Paths.get("chappie.trace." + i + ".csv"));
          Files.move(Paths.get("chappie.thread.csv"), Paths.get("chappie.thread." + i + ".csv"));
        }
      } catch(Exception e) {
        System.out.println("Unable to bootstrap " + args[1]);
      }
    } catch(Exception e) {
      System.out.println("Unable to load " + args[0]);
    }
  }
}
