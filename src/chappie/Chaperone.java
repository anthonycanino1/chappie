/* ************************************************************************************************
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * Copyright 2019 SUNY Binghamton
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

import java.io.*;

import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.ArrayList;

import chappie.profile.ChappieProfiler;

import chappie.glibc.GLIBC;

public class Chaperone implements Runnable {
  // Metrics
  private int epoch;
  private long start;
  private double[] initialRaplReading;

  // Timer
  private Thread thread;

  public enum Mode {NOP, SLEEP, POLL, SAMPLE}
  private Config config;
  public class Config {
    public String workDirectory;
    public String suffix;

    public Mode mode;
    public int timerRate;

    public int vmFactor;
    public int hpFactor;
    public int osFactor;
    public int raplFactor;

    public Config(String workDirectory, String suffix, Mode mode, int timerRate, int vmFactor, int hpFactor, int osFactor, int raplFactor) {
      this.workDirectory = workDirectory;
      this.suffix = suffix;

      this.mode = mode;
      this.timerRate = timerRate;

      this.vmFactor = vmFactor;
      this.hpFactor = hpFactor;
      this.osFactor = osFactor;
      this.raplFactor = raplFactor;
    }

    @Override
    public String toString() {
      String message = "################################";
      message += "\tChaperone Parameters:";
      message += "\n\t - Mode:\t\t\t" + mode;
      if (mode == Mode.SLEEP) {
        message += "\n\t - Sleep Rate:\t\t" + timerRate + " milliseconds";
      } else if (mode == Mode.POLL || mode == Mode.SAMPLE) {
        message += "\n\t - VM Polling Rate:\t\t" + vmFactor * timerRate + " milliseconds";
        message += "\n\t - OS Polling Rate:\t\t" + osFactor * timerRate + " milliseconds";
        message += "\n\t - HP Polling Rate:\t\t" + hpFactor * timerRate + " milliseconds";
        message += "\n\t - RAPL Polling Rate:\t\t" + raplFactor * timerRate + " milliseconds";
      }
      message += "\n################################";

      return message;
    }
  }

  @Override
  public String toString() { return this.config.toString(); }

  // Monitor
  private ChappieProfiler profiler = null;

  public Chaperone() {
    String workDir = System.getProperty("chappie.workDir", "data");
    String suffix = System.getProperty("chappie.suffix", "");

    Mode mode = Mode.valueOf(System.getProperty("chappie.mode", "SAMPLE"));
    int tr = Integer.parseInt(System.getProperty("chappie.timer", "2"));

    int vm = Integer.parseInt(System.getProperty("chappie.vm", "2"));
    int os = Integer.parseInt(System.getProperty("chappie.os", "5"));
    int hp = Integer.parseInt(System.getProperty("chappie.hp", "1"));
    int rapl = Integer.parseInt(System.getProperty("chappie.rapl", "1"));

    config = new Config(workDir, suffix, mode, tr, vm, os, hp, rapl);

    if (this.config.mode != Mode.NOP) {
      profiler = new ChappieProfiler(config);
      thread = new Thread(this, "chappie");
    }

    epoch = 0;
    start = System.currentTimeMillis();
    initialRaplReading = jrapl.EnergyCheckUtils.getEnergyStats();
  }

  private ArrayList<ArrayList<Object>> activity = new ArrayList<ArrayList<Object>>();

  public void run() {
    while (!thread.interrupted()) {
      long startTime = System.nanoTime();
      long epochTime = System.currentTimeMillis();

      if (config.mode == Mode.POLL || config.mode == Mode.SAMPLE) {
        epoch++;
        profiler.sample(epoch, epochTime);
      }

      // estimate of chappie's machine time usage based on runtime
      long readingTime = System.nanoTime() - startTime;
      long millis = readingTime / 1000000;
      int nanos = (int)(readingTime - millis * 1000000);

      long milliSleep = config.timerRate - millis - (nanos > 0 ? 1 : 0);
      int nanoSleep = 1000000 - nanos;

      try {
        if (milliSleep >= 0)
          Thread.sleep(milliSleep, nanoSleep);
      } catch (InterruptedException e) { }

      long totalTime = System.nanoTime() - startTime;

      ArrayList<Object> record = new ArrayList<Object>();

      record.add(epoch);
      record.add(currentTime);
      record.add((double)(totalTime) / 1000000);
      record.add((double)(readingTime) / totalTime);

      activeness.add(record);
    }
  }

  public boolean cancel() {
    if (config.mode == Mode.NOP)
      dump();
    else {
      thread.interrupt();
      try {
        thread.join();
      } catch (Exception e) {
        System.out.println("unable to join chappie: " + ex.getClass().getCanonicalName());
      }
    }

    return true;
  }

  private void dump() {
    if (mode != Mode.NOP) {
      CSVPrinter printer = new CSVPrinter(
        new FileWriter(config.workDirectory + "/chappie.activity" + config.suffix + ".csv"),
        CSVFormat.DEFAULT.withHeader("epoch", "timestamp", "elapsed", "activity")
      );

      printer.printRecords(activity);
      printer.close();
    }

    for (Profiler profiler: profilers)
      profiler.dump();
  }
}
