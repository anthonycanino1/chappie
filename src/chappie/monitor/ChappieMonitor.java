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

package chappie.monitor;

import chappie.input.Config;
import chappie.input.Config.Mode;

import chappie.util.GLIBC;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import java.io.*;

import java.nio.file.Path;
import java.nio.file.Paths;

import jrapl.EnergyCheckUtils.*;

public class ChappieMonitor {
  Config config;

  private double[] initialRaplReading;

  ThreadGroup rootGroup;

  public ChappieMonitor(Config config) {
    this.config = config;

    if (config.raplFactor > 0)
      initialRaplReading = jrapl.EnergyCheckUtils.getEnergyStats();
    else
      initialRaplReading = new double[] {0, 0, 0, 0, 0, 0};

    rootGroup = Thread.currentThread().getThreadGroup();
    ThreadGroup parentGroup;
    while ((parentGroup = rootGroup.getParent()) != null)
        rootGroup = parentGroup;
  }

  // Runtime data containers
  private ArrayList<ArrayList<Object>> threadData = new ArrayList<ArrayList<Object>>();
  private ArrayList<ArrayList<Object>> idData = new ArrayList<ArrayList<Object>>();
  private ArrayList<ArrayList<Object>> jiffiesData = new ArrayList<ArrayList<Object>>();
  private ArrayList<ArrayList<Object>> energyData = new ArrayList<ArrayList<Object>>();

  public void read(int epoch, long unixTime) {
    ArrayList<Object> record;

    // read vm state
    if (config.vmFactor > 0 && epoch % config.vmFactor == 0) {
      Thread[] threads = new Thread[rootGroup.activeCount()];
      while (rootGroup.enumerate(threads, true) == threads.length)
          threads = new Thread[threads.length * 2];

      for (Thread thread: threads)
        if (config.mode == Mode.SAMPLE && thread != null) {
          record = new ArrayList<Object>();

          record.add(epoch);
          record.add(unixTime);
          record.add(thread.getName());
          record.add(thread.getId());
          // record.add(GLIBC.tids.get(thread.getName()));
          record.add(thread.getState() == Thread.State.RUNNABLE);

          idData.add(record);
        }
    }

    // read os state
    if (config.osFactor > 0 && epoch % config.osFactor == 0) {
      for (File f: new File("/proc/" + GLIBC.getProcessId() + "/task/").listFiles()) {
        if (config.mode == Mode.SAMPLE) {
          int tid = Integer.parseInt(f.getName());
          String threadRecord = GLIBC.readThread(tid);
          if (threadRecord.length() > 0) {
            record = new ArrayList<Object>();

            record.add(epoch);
            record.add(tid);
            record.add(unixTime);
            record.add(threadRecord);

            threadData.add(record);
          }
        }
      }

      if (config.mode == Mode.SAMPLE) {
        record = new ArrayList<Object>();
        record.add(GLIBC.readSystemJiffies());
        jiffiesData.add(record);
      }
    }

    // read energy
    if (config.mode == Mode.SAMPLE && config.raplFactor > 0 && epoch % config.raplFactor == 0) {
      double[] raplReading = jrapl.EnergyCheckUtils.getEnergyStats();

      for (int i = 0; i < raplReading.length / 3; ++i) {
        record = new ArrayList<Object>();

        record.add(epoch);
        record.add(i + 1);
        record.add(raplReading[3 * i + 2]);
        record.add(raplReading[3 * i]);

        energyData.add(record);
      }
    }
  }

  public void dump() {
    String directory = config.workPath;
    String suffix = System.getProperty("chappie.suffix", "");
    if (suffix != "")
      suffix = "." + suffix;

    String message;
    PrintWriter log = null;

    // if (config.timerRate > 0) {
      // thread data
      String path = Paths.get(directory, "chappie.thread" + suffix + ".csv").toString();
      try {
        log = new PrintWriter(new BufferedWriter(new FileWriter(path)));
      } catch (Exception io) { }

      message = "epoch,tid,timestamp,record\n";
      log.write(message);

      for (List<Object> frame: threadData) {
        message = "";
        for (Object item: frame) {
          message += item.toString() + ",";
        }
        message = message.substring(0, message.length() - 1);
        message += "\n";

        log.write(message);
      }

      log.close();

      // tid data
      path = Paths.get(directory, "chappie.tid" + suffix + ".csv").toString();
      try {
        log = new PrintWriter(new BufferedWriter(new FileWriter(path)));
      } catch (Exception io) { }

      message = "thread,tid\n";
      log.write(message);

      // System.out.println(GLIBC.tids.toString());

      for (Map.Entry<Thread, Integer> thread: GLIBC.tids.entrySet()) {
        message = thread.getKey().getName() + "," + thread.getValue().toString() + "\n";
        log.write(message);
      }

      log.close();
      GLIBC.tids.clear();

      // id data
      path = Paths.get(directory, "chappie.id" + suffix + ".csv").toString();
      try {
        log = new PrintWriter(new BufferedWriter(new FileWriter(path)));
      } catch (Exception io) { }

      message = "epoch,timestamp,thread,id,state\n";
      log.write(message);

      for (List<Object> frame: idData) {
        message = "";
        for (Object item: frame) {
          message += item.toString() + ",";
        }
        message = message.substring(0, message.length() - 1);
        message += "\n";
        log.write(message);
      }

      log.close();

      // energy data
      path = Paths.get(directory, "chappie.energy" + suffix + ".csv").toString();
      try {
        log = new PrintWriter(new BufferedWriter(new FileWriter(path)));
      } catch (Exception io) { }

      message = "epoch,socket,package,dram\n";
      log.write(message);

      for (List<Object> frame: energyData) {
        message = "";
        for (Object item: frame) {
          message += item.toString() + ",";
        }
        message = message.substring(0, message.length() - 1);
        message += "\n";
        log.write(message);
      }

      log.close();

      // system data
      path = Paths.get(directory, "chappie.system" + suffix + ".csv").toString();
      try {
        log = new PrintWriter(new BufferedWriter(new FileWriter(path)));
      } catch (Exception io) { }

      log.write("record\n");

  	  for (ArrayList<Object> frame : jiffiesData) {
        message = "";
        for (Object item: frame) {
          message += item.toString() + ",";
        }
        message = message.substring(0, message.length() - 1);
        message += "\n";
        log.write(message);
      }

      log.close();
    // }
  }

  public void dumpstats() {
    Runtime rt = Runtime.getRuntime();
    try {
      Process pr = rt.exec("/sbin/m5 dumpstats");
    } catch(Exception exc) {
      exc.printStackTrace();
    }
  }
}
