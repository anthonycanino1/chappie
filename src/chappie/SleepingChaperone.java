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

import chappie.input.Config;
import chappie.input.Config.Mode;

import chappie.monitor.ChappieMonitor;

import chappie.util.GLIBC;

import java.io.*;

import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class SleepingChaperone implements Runnable {
  int mainID;

  // Metrics
  private int epoch;
  private long start;
  private double[] initialRaplReading;

  // Timer
  private Thread thread;

  // Monitor
  private Config config;
  private ChappieMonitor monitor = null;

  public SleepingChaperone(Config config) {
    this.config = config;

    mainID = GLIBC.getProcessId();
    GLIBC.getThreadId();

    if (this.config.mode != Mode.NOP) {
      this.monitor = new ChappieMonitor(config);
      thread = new Thread(this, "Chaperone");
      thread.start();
    }

    epoch = 0;
    start = System.currentTimeMillis();
    initialRaplReading = jrapl.EnergyCheckUtils.getEnergyStats();
  }

  // Runtime data containers
  // the other containers are in the monitor now
  private ArrayList<ArrayList<Object>> activeness = new ArrayList<ArrayList<Object>>();
  private ArrayList<ArrayList<Object>> misses = new ArrayList<ArrayList<Object>>();

  public void run() {
    while (!thread.interrupted()) {
      long elapsed = System.nanoTime();
      long currentTime = System.currentTimeMillis();
      if (config.mode == Mode.POLL || config.mode == Mode.SAMPLE) {
        epoch++;

        // read from the specific monitor
        monitor.read(epoch, currentTime);
      }
      // estimate of chappie's machine time usage based on runtime
      elapsed = System.nanoTime() - elapsed;
      long millis = elapsed / 1000000;
      int nanos = (int)(elapsed - millis * 1000000);
      // System.out.println(elapsed);
      // System.out.println(millis);
      // System.out.println(nanos);
      // long elapsedTime = System.currentTimeMillis() - currentTime;
      // long sleepTime = config.timerRate - elapsedTime;
      try {
        Thread.sleep(millis, nanos);
        // if (sleepTime > 0)
        //   Thread.sleep(sleepTime);
        // else
        //   Thread.sleep(config.timerRate);
      } catch (InterruptedException e) {
        System.out.println("Someone tried to interrupt me!");
        // long sleptTime = System.currentTimeMillis() - currentTime;
        // sleepTime = config.timerRate - sleptTime;
        // try {
        //   if (sleepTime > 0)
        //     Thread.sleep(sleepTime);
        // } catch (InterruptedException e2) { System.out.println("Someone tried to interrupt me again!!"); }
        break;
      } finally {
        if (config.mode != Mode.NOP) {
          long totalTime = System.currentTimeMillis() - currentTime;

          ArrayList<Object> record = new ArrayList<Object>();

          record.add(epoch);
          record.add(currentTime);
          record.add(totalTime);
          record.add((double)(millis) / totalTime);
          // record.add((double)(elapsedTime) / totalTime);

          activeness.add(record);
        }
      }
    }
  }

  public boolean cancel() {
    // use the double flag to kill the process from in here
    // terminated = true;
    if (config.mode != Mode.NOP) {
      thread.interrupt();
      try {
        thread.join();
      } catch (Exception e) {
        System.out.println("Something bad happened here!");
      }
    }

    dump();
    if (config.mode == Mode.POLL || config.mode == Mode.SAMPLE)
      monitor.dump();
    return true;
  }

  private void dump() {
    String directory = config.workPath;
    String suffix = System.getProperty("chappie.suffix", "");
    if (suffix != "")
      suffix = "." + suffix;

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

    // chappie activeness
    if (config.mode != Mode.NOP) {
      path = Paths.get(directory, "chappie.activeness" + suffix + ".csv").toString();
      try {
        log = new PrintWriter(new BufferedWriter(new FileWriter(path)));
      } catch (Exception io) { }

      log.write("epoch,timestamp,total,activeness\n");

      for (ArrayList<Object> frame : activeness) {
        message = "";
        for (Object item: frame) {
          message += item.toString() + ",";
        }
        message = message.substring(0, message.length() - 1);
        message += "\n";
        log.write(message);
      }
      log.close();
    }
  }
}
