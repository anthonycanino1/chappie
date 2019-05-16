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

// import chappie.monitor.JDK9Monitor;
// import chappie.monitor.NOPMonitor;
import chappie.util.GLIBC;

import java.io.*;

import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class Chaperone extends TimerTask {
  int mainID;

  // Metrics
  private int epoch;
  private long start;
  private long lastScheduledTime;
  private double[] initialRaplReading;

  // Timer
  private Timer timer;

  // Monitor
  private Config config;
  private ChappieMonitor monitor = null;

  public Chaperone(Config config) {
    this.config = config;

    if (this.config.mode != Mode.SAMPLE)
      this.config.timerRate = -1;

    mainID = GLIBC.getProcessId();

    this.monitor = new ChappieMonitor(config);
    timer = new Timer("Chaperone");
    if (this.config.timerRate > 0)
      timer.scheduleAtFixedRate(this, 100, this.config.timerRate);
    else
      terminated = true;

    epoch = 0;
    start = System.currentTimeMillis();
    lastChappieTime = start;
    initialRaplReading = jrapl.EnergyCheckUtils.getEnergyStats();
  }

  // Runtime data containers
  // the other containers are in the monitor now
  private ArrayList<ArrayList<Object>> activeness = new ArrayList<ArrayList<Object>>();
  private ArrayList<ArrayList<Object>> misses = new ArrayList<ArrayList<Object>>();

  // TIMER TASK CLASS METHODS

  // Termination flags
  // The timer class does not have a synchronized termination method. Luckily,
  // only main touches the chaperone, so we can use a double flag psuedo-lock.
  private boolean terminate = false;
  private boolean terminated = false;

  private long lastChappieTime = 0;

  @Override
  public void run() {
    // // Check if we need to stop
    // boolean halt=false;
    //
    // if(epoch >= early_exit && early_exit > 0) {
    //   terminate=true;
    //   terminated=true;
    //   halt=true;
    // }

    long currentEpochTime = System.currentTimeMillis();
    long elapsedTime = scheduledExecutionTime() - lastScheduledTime;
    System.out.println(elapsedTime);
    if (!terminate) {
      if(elapsedTime >= config.timerRate) {
        // cache last epoch's ms timestamp
        lastScheduledTime = scheduledExecutionTime();

        if (epoch > 0) {
          ArrayList<Object> record = new ArrayList<Object>();
          record.add(epoch);
          record.add(currentEpochTime);
          record.add((double)(lastChappieTime) / elapsedTime);
          activeness.add(record);
        }

        // read from the specific monitor
        monitor.read(epoch);

        // estimate of chappie's machine time usage based on runtime
        lastChappieTime = System.currentTimeMillis() - currentEpochTime;
      } else {
        ArrayList<Object> record = new ArrayList<Object>();

        record.add(epoch);
        record.add(currentEpochTime);
        record.add(elapsedTime);

        misses.add(record);
      }
    } else {
      // stop ourselves before letting everything know we're done
      terminated = true;

      ArrayList<Object> record = new ArrayList<Object>();
      record.add(epoch);
      record.add(currentEpochTime);
      record.add((double)(lastChappieTime) / elapsedTime);
      activeness.add(record);

      // if(halt) {
      //   cancel();
      //   Runtime.getRuntime().halt(0);
      // }
    }

    epoch++;
  }

  @Override
  public boolean cancel() {
    // use the double flag to kill the process from in here
    terminate = true;

    while(!terminated) {
      try {
        Thread.sleep(0, 100);
      } catch(Exception e) { }
    }
    super.cancel();
    timer.cancel();

    dump();
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
    if (config.timerRate > 0) {
      path = Paths.get(directory, "chappie.activeness" + suffix + ".csv").toString();
      try {
        log = new PrintWriter(new BufferedWriter(new FileWriter(path)));
      } catch (Exception io) { }

      log.write("epoch,timestamp,activeness\n");

      message = "";
      for (ArrayList<Object> frame : activeness) {
        for (Object item: frame) {
          message += item.toString() + ",";
        }
        message = message.substring(0, message.length() - 1);
        message += "\n";
        log.write(message);
      }
      log.close();

      path = Paths.get(directory, "chappie.misses" + suffix + ".csv").toString();
      try {
        log = new PrintWriter(new BufferedWriter(new FileWriter(path)));
      } catch (Exception io) { }

      log.write("epoch,timestamp,tardiness\n");

      message = "";
      for (ArrayList<Object> frame : misses) {
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
