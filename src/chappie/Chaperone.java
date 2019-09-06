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

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import chappie.profile.*;

import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVFormat;

public class Chaperone implements Runnable {
  private int epoch = 0;

  private Thread thread;

  public enum Mode {NOP, SLEEP, POLL, SAMPLE}

  private Config config;
  public static class Config {
    public String workDirectory;
    public String suffix;

    public Mode mode;
    public int timerRate;

    public int vmFactor;
    public int hpFactor;
    public int osFactor;
    public int raplFactor;

    public Config(
      String workDirectory,
      Mode mode,
      int timerRate,
      int vmFactor,
      int osFactor,
      int hpFactor,
      int raplFactor
    ) {
      this.workDirectory = workDirectory;

      this.mode = mode;
      this.timerRate = timerRate;

      this.vmFactor = vmFactor;
      this.osFactor = osFactor;
      this.hpFactor = hpFactor;
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

  public static Config parseConfig() {
    String workDir = System.getProperty("chappie.dir", "data");

    Mode mode = Mode.valueOf(System.getProperty("chappie.mode", "SAMPLE"));
    int tr = Integer.parseInt(System.getProperty("chappie.rate", "2"));

    int vm = Integer.parseInt(System.getProperty("chappie.vm", "2"));
    int os = Integer.parseInt(System.getProperty("chappie.os", "5"));
    int hp = Integer.parseInt(System.getProperty("chappie.hp", "1"));
    int rapl = Integer.parseInt(System.getProperty("chappie.rapl", new Integer(vm).toString()));

    Config config = new Chaperone.Config(workDir, mode, tr, vm, os, hp, rapl);

    return config;
  }

  @Override
  public String toString() { return this.config.toString(); }

  private ArrayList<Profiler> profilers = new ArrayList<Profiler>();

  public Chaperone() {
    config = parseConfig();
    if (config.mode != Mode.NOP) {
      // profiler.add(new RuntimeProfiler(config));
    }

    thread = new Thread(this, "chappie");
  }

  private static class ActivityRecord {
    int epoch; long timestamp; double elapsedTime; double totalTime;

    public ActivityRecord(int epoch, long timestamp, long elapsedTime, long totalTime) {
      this.epoch = epoch;
      this.timestamp = timestamp;
      this.elapsedTime = elapsedTime;
      this.totalTime = totalTime;
    }
  }

  private ArrayList<ActivityRecord> data = new ArrayList<ActivityRecord>();

  public void run() {
    if (config.mode != Mode.NOP) {
      long timestamp = System.currentTimeMillis();
      for (Profiler profiler: profilers)
        profiler.sample(epoch, timestamp);
    } else {
      while (!thread.interrupted()) {
        long timestamp = System.currentTimeMillis();
        long start = System.nanoTime();

        if (config.mode == Mode.POLL || config.mode == Mode.SAMPLE) {
          epoch++;
          for (Profiler profiler: profilers)
            profiler.sample(epoch, timestamp);
        }

        long elapsed = System.nanoTime() - start;
        long millis = elapsed / 1000000;
        int nanos = (int)(elapsed - millis * 1000000);

        millis = config.timerRate - millis - (nanos > 0 ? 1 : 0);
        nanos = 1000000 - nanos;

        if (millis >= 0 && nanos > 0)
          try {
            Thread.sleep(millis, nanos);
          } catch (InterruptedException e) {

          }

        long total = System.nanoTime() - start;
        data.add(new ActivityRecord(epoch, timestamp, elapsed, total));
      }

      try {
        dump();
      } catch (IOException io) { }
    }
  }

  public boolean cancel() {
    if (config.mode == Mode.NOP)
      try {
        dump();
      } catch (IOException io) { }
    else {
      thread.interrupt();
      try {
        thread.join();
      } catch (Exception e) {
        System.out.println("unable to join chappie: " + e.getClass().getCanonicalName());
      }
    }

    return true;
  }

  private void dump() throws IOException {
    if (config.mode != Mode.NOP) {
      CSVPrinter printer = new CSVPrinter(
        new FileWriter(config.workDirectory + "activity.csv"),
        CSVFormat.DEFAULT.withHeader("epoch", "timestamp", "elapsed", "activity")
      );

      printer.printRecords(data);
      printer.close();
    }

    for (Profiler profiler: profilers)
      profiler.dump();
  }
}
