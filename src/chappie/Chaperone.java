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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

import chappie.profile.*;
import chappie.profile.impl.*;
import chappie.profile.util.*;
import chappie.util.ChappieLogger;

public class Chaperone implements Runnable {
  // used for tracking instances; in reality, this is a singleton class since
  // there is no reason to have a second chaperone. even in the case of
  // parallel sampling, the singleton should manage the system.
  private static int id = 0;

  private static String baseWorkDirectory;
  private static String workDirectory;
  public static String getWorkDirectory() {
    return workDirectory;
  }

  private int timerRate;
  private int epoch = 0;

  public Thread thread;
  private Logger logger;

  private ArrayList<Profiler> profilers = new ArrayList<Profiler>();

  public Chaperone() {
    logger = ChappieLogger.buildLogger();

    // grab the work directory if it doesn't exist yet and then make a new one
    if (baseWorkDirectory == null) {
      baseWorkDirectory = System.getProperty("chappie.dir", "chappie-logs") + "/raw";
      logger.info("base work directory set to " + baseWorkDirectory);
    }
    workDirectory = baseWorkDirectory + "/" + id;
    new File(workDirectory).mkdir();

    logger.info("creating chappie instance " + ++id);
    // check if we are in nop
    timerRate = Integer.parseInt(System.getProperty("chappie.rate", "1"));
    if (timerRate > 0) {
      // setup the various profilers
      int vmRate = Integer.parseInt(System.getProperty("chappie.vm", "1"));
      if (vmRate > 0)
        profilers.add(new VMProfiler(vmRate, timerRate, workDirectory));

      int osRate = Integer.parseInt(System.getProperty("chappie.os", "1"));
      if (osRate > 0)
        profilers.add(new OSProfiler(osRate, timerRate, workDirectory));

      int raplRate = Integer.parseInt(System.getProperty("chappie.rapl", "1"));
      if (raplRate > 0)
        profilers.add(new RAPLProfiler(raplRate, timerRate, workDirectory));

      int traceRate = Integer.parseInt(System.getProperty("chappie.trace", "1"));
      if (traceRate > 0)
        profilers.add(new TraceProfiler(traceRate, timerRate, workDirectory));

      thread = new Thread(this, "chappie-" + id);
    } else {
      // we probably need a runtime profiler to collect nop stats
      profilers.add(new RAPLProfiler(0, 0, workDirectory));
      logger.info("running in nop mode");
    }
  }

  // Thread-like interface since we don't deal with the structure like a
  // runnable; I should find out if there's a better pattern
  public void start() {
    if (timerRate > 0) {
      thread.start();
    } else {
      timestamps.put(epoch++, System.currentTimeMillis());

      for (Profiler profiler: profilers)
        profiler.sample(epoch);
    }

    logger.info("starting profiling");
  }

  public void stop() {
    if (timerRate > 0) {
      thread.interrupt();

      try {
        thread.join();
      } catch(InterruptedException e) {
        logger.info("chappie couldn't join: " + e.getMessage());
      }
    } else {
      timestamps.put(epoch++, System.currentTimeMillis());

      for (Profiler profiler: profilers)
        profiler.sample(epoch);
    }

    try {
      dump();
    } catch(IOException io) {
      logger.info("couldn't write data: " + io.getMessage());
    }
  }

  // measurement of chappie's activeness for each epoch
  private static class ChappieRecord extends Record {
    private long elapsed;
    private long total;

    public ChappieRecord(int epoch, long elapsed, long total) {
      this.epoch = epoch;
      this.elapsed = elapsed;
      this.total = total;
    }

    protected String stringImpl() {
      return elapsed + ";" + total;
    }

    private static final String[] header = new String[] { "epoch", "elapsed", "total" };
    public static String[] getHeader() {
      return header;
    };
  }

  private ArrayList<Record> data = new ArrayList<Record>();
  private HashMap<Integer, Long> timestamps = new HashMap<Integer, Long>();
  public void run() {
    while (!thread.interrupted()) {
      // in addition to sampling all profilers, chappie needs to
      // know when the samples are taken, how long the sampling took,
      // and how long the entire epoch took
      long start = System.nanoTime();

      // epoch++;
      timestamps.put(epoch++, System.currentTimeMillis());
      // timestamps.put(epoch++, start);

      for (Profiler profiler: profilers)
        profiler.sample(epoch);

      long elapsed = System.nanoTime() - start;

      // we try our best to sample at uniform intervals, even when terminating
      try {
        ThreadUtil.sleepUntil(start, timerRate);
      } catch (InterruptedException e) {
        try { ThreadUtil.sleepUntil(start, timerRate); } catch (InterruptedException ex) { }
        break;
      }

      long total = System.nanoTime() - start;
      data.add(new ChappieRecord(epoch, elapsed, total));
    }
  }

  private void dump() throws IOException {
    logger.info("writing chappie data");

    CSV.write(data, ChappieRecord.getHeader(), Chaperone.getWorkDirectory() + "/chappie.csv");
    JSON.write(timestamps, Chaperone.getWorkDirectory() + "/time.json");

    for (Profiler profiler: profilers)
      profiler.dump();

    logger.info("done writing data");
  }
}
