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

  public static int epoch = 0;
  private int mode = 2;
  private int polling = 1000;
  private int coreRate = 2000;
  private boolean readMemory = true;

  public Chaperone(int mode, int polling, int coreRate, boolean memory) {
    Chaperone.epoch = 0;
    this.mode = mode;
    this.polling = Math.min(999999, polling * 1000);
    this.coreRate =  coreRate;
    this.readMemory = memory;

    GLIBC.getProcessId();
    bean = (com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();

    thread = new Thread(this, "Chaperone");
    thread.start();
  }

  protected List<List<Object>> threads = new ArrayList<List<Object>>();
  protected List<List<Object>> energy = new ArrayList<List<Object>>();

  public void run() {
    List<Object> measure;

    long start = System.nanoTime();

    while(!thread.isInterrupted()) {
      Set<Thread> threadSet = Thread.getAllStackTraces().keySet();

      long current = System.nanoTime() - start;

      for(Thread thread: threadSet) {
        if (mode > 1) {
          measure = new ArrayList<Object>();

          measure.add(epoch);

          String name = thread.getName();
          measure.add(name);

          if ((epoch % coreRate) == 0) {
            measure.add(GLIBC.getOSStats(name)[0]);
          } else {
            measure.add("");
          }

          if (mode == 2) {
            measure.add(GLIBC.getOSStats(name)[1]);
            measure.add(GLIBC.getOSStats(name)[2]);
          }

          if (mode == 3) {
            if (thread.getState() == Thread.State.RUNNABLE)
              measure.add(true);
            else
              measure.add(false);
          }

          if (readMemory) {
            measure.add(bean.getThreadAllocatedBytes(Thread.currentThread().getId()));
          }

	  // For Khaled: Modification to make
	  // StrackTraceElement e = thread.getStackTrace()[];
	  // e goes into measure

          threads.add(measure);

	  
        }
      }

      if (mode > 1) {
        int[] jiffies = GLIBC.getJiffies();
        double[] reading = jrapl.EnergyCheckUtils.getEnergyStats();

        for (int i = 0; i < reading.length / 3; ++i) {
          measure = new ArrayList<Object>();

          measure.add(epoch);
          measure.add(current);

          measure.add(i + 1);
          measure.add(reading[3 * i + 2]);
          measure.add(reading[3 * i]);

          measure.add(jiffies[0]);
          measure.add(jiffies[1]);

          energy.add(measure);
        }
      }

      epoch++;

      try {
        Thread.sleep(0, polling);
      } catch (InterruptedException e) { Thread.currentThread().interrupt();}
    }

    long now = System.nanoTime() - start;

    System.out.println("Execution took " + (double)now / 1000000000.0 + " seconds");

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
    if (mode > 1) {
      PrintWriter log = null;

      String path = System.getenv("CHAPPIE_TRACE_LOG");
      if (path == null)
        path = "chappie.trace.csv";

      try {
        log = new PrintWriter(new BufferedWriter(new FileWriter(path)));
      } catch (Exception io) {
        System.err.println("Error: " + io.getMessage());
      }

      String message = "";

      message = "epoch,time,socket,package,dram,u_jiffies,k_jiffies\n";

      log.write(message);
      for (List<Object> frame: energy) {
        message = "";
        for (Object o: frame)
          message += o.toString() + ",";
        message = message.substring(0, message.length() - 1);
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

      if (mode == 2) {
        message = "epoch,thread,core,u_jiffies,k_jiffies";
      } else if (mode == 3) {
        message = "epoch,thread,core,state";
      }

      if (readMemory) {
        message += ",bytes";
      }

      message += "\n";

      log.write(message);
      for (List<Object> frame : threads) {
        message = "";
        for (Object o: frame)
          message += o.toString() + ",";
        message = message.substring(0, message.length() - 1);
        message += "\n";
        log.write(message);
      }
      log.close();

      path = System.getenv("CHAPPIE_STACK_LOG");
      if (path == null)
        path = "chappie.stack.txt";

      try {
        log = new PrintWriter(new BufferedWriter(new FileWriter(path)));
      } catch (Exception io) {
        System.err.println("Error: " + io.getMessage());
      }

      try {
	 chappie.util.StatsUtil.print_method_stats();
      } catch(Exception exception) {
	      exception.printStackTrace();
      }

      for (String thread : GLIBC.callsites.keySet()) {
        message = thread;
        for (StackTraceElement e: GLIBC.callsites.get(thread))
          message += "," + e.toString();
        message += "\n";
        log.write(message);
      }
      log.close();
    }
  }

  public static void main(String[] args) throws IOException {
    Integer iterations = 10;
    try {
      iterations = Integer.parseInt(System.getenv("ITERS"));
    } catch(Exception e) { }

    Integer mode = 3;
    try {
      mode = Integer.parseInt(System.getenv("MODE"));
    } catch(Exception e) { }

    Integer polling = 1000;
    try {
      polling = Integer.parseInt(System.getenv("POLLING"));
    } catch(Exception e) { }

    Integer coreRate = 1;
    try {
      coreRate = Integer.parseInt(System.getenv("CORE_RATE"));
    } catch(Exception e) { }

    Integer readMemory = 1;
    try {
      readMemory = Integer.parseInt(System.getenv("MEMORY"));
    } catch(Exception e) { }

    System.out.println("Chaperone Parameters: Mode " + mode + ", Polling Rate " + polling + " microseconds, Core reading rate " + coreRate + ", Memory Readings " + (readMemory == 1));

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

          Chaperone chaperone = new Chaperone(mode, polling, coreRate, readMemory == 1);
          main.invoke(null, (Object)params.toArray(new String[params.size()]));
          System.out.println("==================================================");
          System.out.println("Dismissing the chaperone");
          chaperone.dismiss();
          Files.move(Paths.get("chappie.trace.csv"), Paths.get("chappie.trace." + i + ".csv"));
          Files.move(Paths.get("chappie.thread.csv"), Paths.get("chappie.thread." + i + ".csv"));
          Files.move(Paths.get("chappie.stack.txt"), Paths.get("chappie.stack." + i + ".txt"));
          Files.move(Paths.get("method_stats.csv"), Paths.get("method_stats." + i + ".txt"));
        }
      } catch(Exception e) {
        System.out.println("Unable to bootstrap " + args[1] + ": " + e);
      }
    } catch(Exception e) {
      System.out.println("Unable to load " + args[0] + ": " + e);
    }
  }
}
