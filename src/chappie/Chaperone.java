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

import java.util.Timer;
import java.util.TimerTask;

public class Chaperone extends TimerTask {
  public enum Mode {NOP, NAIVE, OS_NAIVE, OS_SAMPLE, VM_SAMPLE, FULL_SAMPLE}

  public static int epoch = 0;

  private long start;

  private Timer timer;
  private com.sun.management.ThreadMXBean bean;

  private int polling;
  private int coreReadingFactor;

  private Mode mode;
  private boolean readMemory;
  private boolean instrumentMethods;
  private boolean printStackTrace;

  ThreadGroup rootThreadGroup;

  public Chaperone(Mode mode, int polling, int coreRate, boolean memory, boolean instrument, boolean printStack) {
    this.polling = polling;
    this.coreReadingFactor = coreRate;

    this.mode = mode;
    this.readMemory = memory;
    this.instrumentMethods = instrument;
    this.printStackTrace = printStack;

    Chaperone.epoch = 0;
    bean = (com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();

    rootThreadGroup = Thread.currentThread().getThreadGroup();
    while (rootThreadGroup.getParent() != null)
      rootThreadGroup = rootThreadGroup.getParent();

    start = System.nanoTime();

    if (mode != Mode.NOP) {
      timer = new Timer("Chaperone");
      timer.scheduleAtFixedRate(this, 0, polling);
    }
  }

  protected List<List<Object>> threads = new ArrayList<List<Object>>();
  protected List<List<Object>> energy = new ArrayList<List<Object>>();

  private boolean terminate = false;
  private boolean terminated = false;

  @Override
  public void run() {
    if (!terminate) {
      List<Object> measure;
      long elapsed = System.nanoTime() - start;

      if (mode != Mode.NOP && mode != Mode.NAIVE && mode != Mode.OS_NAIVE) {
        int capacity = rootThreadGroup.activeCount() + 1;
        Thread[] threadArray = new Thread[capacity];
        while(rootThreadGroup.enumerate(threadArray, true) == capacity) {
          capacity *= 2;
          threadArray = new Thread[capacity];
        }
        
        //for(Thread thread: threadArray) {
        Map<Thread, ?> threadStacks = Thread.getAllStackTraces();
        for(Thread thread: threadStacks.keySet()) {
          if (thread != null) {
            measure = new ArrayList<Object>();
            measure.add(epoch);

            String name = thread.getName();
            measure.add(name);

            if ((epoch % coreReadingFactor) == 0)
              measure.add(GLIBC.getOSStats(name)[0]);
            else
              measure.add("");

            if (mode == Mode.OS_SAMPLE || mode == Mode.FULL_SAMPLE) {
              measure.add(GLIBC.getOSStats(name)[1]);
              measure.add(GLIBC.getOSStats(name)[2]);
            }

            if (mode == Mode.VM_SAMPLE || mode == Mode.FULL_SAMPLE)
              if (thread.getState() == Thread.State.RUNNABLE)
                measure.add(true);
              else
                measure.add(false);

        		if (readMemory) measure.add(bean.getThreadAllocatedBytes(Thread.currentThread().getId()));

          	// if (printStackTrace) measure.add(thread.getStackTrace());
            if (printStackTrace) measure.add(GLIBC.peekStack(thread));

            threads.add(measure);
          }
        }

        if (mode != Mode.NOP && mode != Mode.NAIVE && mode != Mode.OS_NAIVE) {
          int[] jiffies = GLIBC.getJiffies();
          double[] reading = jrapl.EnergyCheckUtils.getEnergyStats();

          long readingTime = (System.nanoTime() - start) - elapsed;

          for (int i = 0; i < reading.length / 3; ++i) {
            measure = new ArrayList<Object>();

            measure.add(epoch);
            measure.add(elapsed);
            measure.add(readingTime);

            measure.add(i + 1);
            measure.add(reading[3 * i + 2]);
            measure.add(reading[3 * i]);

            measure.add(jiffies[0]);
            measure.add(jiffies[1]);

            energy.add(measure);
          }
        }

        epoch++;
      }
    } else {
      timer.cancel();
      terminated = true;
    }
  }

  public void dismiss() {
    if (mode != Mode.NOP) {
      terminate = true;
      while(!terminated) {
        try {
          Thread.sleep(0, 100);
        } catch(Exception e) { }
      }
    }
    retire();
  }

  private void retire() {
    PrintWriter log = null;
    String path = "chappie.runtime.csv";
    String message = "" + (System.nanoTime() - start);

    try {
      log = new PrintWriter(new BufferedWriter(new FileWriter(path)));
    } catch (Exception io) {
      System.err.println("Error: " + io.getMessage());
    }

    log.write(message);
    log.close();

    if (mode != Mode.NOP && mode != Mode.NAIVE && mode != Mode.OS_NAIVE) {
      path = "chappie.trace.csv";
      try {
        log = new PrintWriter(new BufferedWriter(new FileWriter(path)));
      } catch (Exception io) {
        System.err.println("Error: " + io.getMessage());
      }

      message = "epoch,time,diff,socket,package,dram,u_jiffies,k_jiffies\n";
      log.write(message);

      for (List<Object> frame: energy) {
        message = "";
        for (Object item: frame)
          message += item.toString() + ",";
        message = message.substring(0, message.length() - 1);
        message += "\n";
        log.write(message);
      }
      log.close();

      path = "chappie.thread.csv";

      try {
        log = new PrintWriter(new BufferedWriter(new FileWriter(path)));
      } catch (Exception io) {
        System.err.println("Error: " + io.getMessage());
      }

      if (mode == Mode.OS_SAMPLE) {
        message = "epoch,thread,core,u_jiffies,k_jiffies";
      } else if (mode == Mode.VM_SAMPLE) {
        message = "epoch,thread,core,state";
      } else if (mode == Mode.FULL_SAMPLE) {
        message = "epoch,thread,core,u_jiffies,k_jiffies,state";
      }

      if (readMemory)
        message += ",bytes";

      if(printStackTrace)
        message += ",stack";

      message += "\n";

      log.write(message);
      for (List<Object> frame : threads) {
        message = "";

      for (Object item: frame)
        if (item instanceof Object[]) {
          Object[] stack = (Object[])item;
          message += stack.length + ";";
          for (Object o: stack)
            message += o.toString() + ";";
        } else
          message += item.toString() + ",";

        message = message.substring(0, message.length() - 1);
        message += "\n";
        log.write(message);
      }
      log.close();

      path = "chappie.stack.txt";
      try {
        log = new PrintWriter(new BufferedWriter(new FileWriter(path)));
      } catch (Exception io) {
        System.err.println("Error: " + io.getMessage());
      }

      for (String thread : GLIBC.callsites.keySet()) {
        message = thread;
        for (StackTraceElement element: GLIBC.callsites.get(thread))
          message += "," + element.toString();
        message += "\n";
        log.write(message);
      }
      log.close();

      if(instrumentMethods) {
        try {
          StatsUtil.print_method_stats();
        } catch(Exception e) { }
      } else {
          path = "method_stats.csv";

          try {
            log = new PrintWriter(new BufferedWriter(new FileWriter(path)));
          } catch (Exception io) {
            System.err.println("Error: " + io.getMessage());
          }

          log.write("No instrumentation present.\n");
          log.close();
      }
    }
  }

  public static void main(String[] args) throws IOException {
    GLIBC.getProcessId();
    GLIBC.getThreadId();


    int iterations = 10;
    try {
      iterations = Integer.parseInt(System.getenv("ITERS"));
    } catch(Exception e) { }

    Mode mode = Mode.VM_SAMPLE;
    try {
      System.out.println(System.getenv("MODE"));
      mode = Mode.valueOf(System.getenv("MODE"));
    } catch(Exception e) { }

    int polling = 1;
    try {
      polling = Integer.parseInt(System.getenv("POLLING"));
    } catch(Exception e) { }

    int coreRate = 1;
    try {
      coreRate = Integer.parseInt(System.getenv("CORE_RATE"));
    } catch(Exception e) { }

    boolean readMemory = true;
    try {
      readMemory = Boolean.parseBoolean(System.getenv("MEMORY"));
      System.out.println(readMemory);
    } catch(Exception e) { }

    boolean instrument = false;
    try {
      instrument = Boolean.parseBoolean(System.getenv("INSTRUMENT"));
    } catch(Exception e) { }

    boolean printStack = false;
    try {
      printStack = Boolean.parseBoolean(System.getenv("STACK_PRINT"));
    } catch(Exception e) { }

    System.out.println("Number of Iterations : " + iterations);
    System.out.println("Chaperone Parameters:" +
                        "\n - Mode:\t\t\t" + mode +
                        "\n - Polling Rate:\t\t" + polling + " milliseconds" +
                        "\n - Core Reading Factor:\t\t" + coreRate +
                        "\n - Memory Readings:\t\t" + readMemory +
                        "\n - Method Instrumentation:\t" + instrument +
                        "\n - Print Stack Trace:\t\t" + printStack);

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

          long start = System.nanoTime();
          Chaperone chaperone = new Chaperone(mode, polling, coreRate, readMemory, instrument, printStack);
          main.invoke(null, (Object)params.toArray(new String[params.size()]));

      	  System.out.println("==================================================");
          System.out.println("Dismissing the chaperone");
          System.out.println(args[1] + " ran in " + String.format("%4f", (double)(System.nanoTime() - start) / 1000000000) + " seconds");
          chaperone.dismiss();

          try {
            Files.move(Paths.get("chappie.runtime.csv"), Paths.get("chappie.runtime." + i + ".csv"));
            Files.move(Paths.get("chappie.trace.csv"), Paths.get("chappie.trace." + i + ".csv"));
            Files.move(Paths.get("chappie.thread.csv"), Paths.get("chappie.thread." + i + ".csv"));
            Files.move(Paths.get("chappie.stack.txt"), Paths.get("chappie.stack." + i + ".txt"));
            Files.move(Paths.get("method_stats.csv"), Paths.get("chappie.methods." + i + ".csv"));
          } catch(Exception e) { }
        }
      } catch(Exception e) {
        System.out.println("Unable to bootstrap " + args[1] + ": " + e);
        e.printStackTrace();
      }
    } catch(Exception e) {
      System.out.println("Unable to load " + args[0] + ": " + e);
      e.printStackTrace();
    }
  }
}
