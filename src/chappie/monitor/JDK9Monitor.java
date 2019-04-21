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

import chappie.util.GLIBC;

import java.util.List;
import java.util.ArrayList;

import java.io.*;

import java.nio.file.Path;
import java.nio.file.Paths;

import jrapl.EnergyCheckUtils.*;

public class JDK9Monitor {
  // params
  int osPolling;

  private boolean no_rapl = false;
  private boolean dump_stats = false;
  private int sockets_no = 0;

  // file helpers
  private String directory;
  private String suffix;

  private double[] initialRaplReading;

  public JDK9Monitor(int osPolling, boolean no_rapl, boolean dump_stats, int sockets_no) {
    // should be handled by highest level call (grid search)
    this.osPolling = osPolling;

    this.no_rapl=no_rapl;
    if (no_rapl) {
      this.sockets_no=sockets_no;
      initialRaplReading = new double[3*sockets_no];
      for(int i=0; i<3*sockets_no;i++)
        initialRaplReading[i] = -2;
    } else {
      initialRaplReading = jrapl.EnergyCheckUtils.getEnergyStats();
    }

    this.dump_stats=dump_stats;

    // definition handled by parent caller (./chappie_test.sh)
    // directory management HAS to be handled by bootstrapper (./run.sh)
    // because of honest profiler (log path)
    directory = System.getenv("CHAPPIE_DIRECTORY");
    directory = directory != null ? directory : "";

    // helper due to the cold run problem we experienced around dacapo
    suffix = System.getenv("CHAPPIE_SUFFIX");
    suffix = suffix != null ? "." + suffix : "";

  }

  // Runtime data containers
  private ArrayList<ArrayList<Object>> threadData = new ArrayList<ArrayList<Object>>();
  private ArrayList<ArrayList<Object>> idData = new ArrayList<ArrayList<Object>>();
  private ArrayList<String> jiffiesData = new ArrayList<String>();
  private ArrayList<ArrayList<Object>> energyData = new ArrayList<ArrayList<Object>>();

  public void read(int epoch) {
    ArrayList<Object> measure;

    // needed for method alignment
    long unixTime = System.currentTimeMillis();

    if (epoch % osPolling == 0) {
      // Read jiffies of the application
      for (File f: new File("/proc/" + GLIBC.getProcessId() + "/task/").listFiles()) {
        measure = new ArrayList<Object>();
        int tid = Integer.parseInt(f.getName());

        measure.add(epoch);
        measure.add(tid);
        measure.add(unixTime);
        measure.add(GLIBC.readThread(tid));

        threadData.add(measure);
      }
    }

    // Read the java ids of all live threads
    ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
    ThreadGroup parentGroup;
    while ((parentGroup = rootGroup.getParent()) != null)
        rootGroup = parentGroup;

    Thread[] threads = new Thread[rootGroup.activeCount()];
    while (rootGroup.enumerate(threads, true ) == threads.length)
        threads = new Thread[threads.length * 2];

    for (Thread thread: threads)
      if (thread != null) {
        measure = new ArrayList<Object>();

        measure.add(epoch);
        measure.add(unixTime);
        measure.add(thread.getName());
        measure.add(thread.getId());
        measure.add(thread.getState());

        idData.add(measure);
      }

    // Read jiffies of system
    if (epoch % osPolling == 0) {
      jiffiesData.add(GLIBC.readSystemJiffies());
    }

    // Read energy of system
    double[] raplReading = new double[0];

		if(no_rapl) {
      raplReading = initialRaplReading;
		} else {
      raplReading = jrapl.EnergyCheckUtils.getEnergyStats();
		}

    for (int i = 0; i < raplReading.length / 3; ++i) {
      measure = new ArrayList<Object>();

      measure.add(epoch);
      measure.add(i + 1);
      measure.add(raplReading[3 * i + 2]);
      measure.add(raplReading[3 * i]);

      energyData.add(measure);
      // }
    }
  }

  public void dump(long start, List<Double> activeness, int mainID) {
    // runtime stats
    PrintWriter log = null;

    String path = Paths.get(directory, "chappie.runtime" + suffix + ".csv").toString();
    try {
      log = new PrintWriter(new BufferedWriter(new FileWriter(path)));
    } catch (Exception io) { }

    long runtime = System.currentTimeMillis() - start;
    double[] raplReading = jrapl.EnergyCheckUtils.getEnergyStats();

    double package1 = raplReading[2] - initialRaplReading[2];
    double package2 = raplReading[5] - initialRaplReading[5];
    double dram1 = raplReading[0] - initialRaplReading[0];
    double dram2 = raplReading[3] - initialRaplReading[3];

    String message = "name,value\nruntime," + runtime +
                      "\nmain_id," + mainID +
                      "\npackage1," + package1 +
                      "\npackage2," + package2 +
                      "\ndram1," + dram1 +
                      "\ndram2," + dram2;

    log.write(message);
    log.close();

    // thread data
    path = Paths.get(directory, "chappie.thread" + suffix + ".csv").toString();
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

	  for (String record : jiffiesData)
      log.write(record + "\n");

    log.close();

    // activeness
    path = Paths.get(directory, "chappie.activeness" + suffix + ".csv").toString();
    try {
      log = new PrintWriter(new BufferedWriter(new FileWriter(path)));
    } catch (Exception io) { }

    log.write("epoch,activeness\n");

    int epoch = 0;
    for (Double activity : activeness) {
      log.write(epoch++ + "," + activity + "\n");
    }
    log.close();

    // path = Paths.get(directory, "chappie.thread" + suffix + ".csv").toString();
    // File file = new File(path);
    //
    // BufferedReader reader = null;
    //
    // try {
    //   reader = new BufferedReader(new FileReader(file));
    //
    //   String text = null;
    //   while ((text = reader.readLine()) != null) {
    //     System.out.println(text);
    //   }
    //
    //   reader.close();
    // } catch (Exception e) { }
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
