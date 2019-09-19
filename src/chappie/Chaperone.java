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
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.stream.Collectors;

import chappie.profile.*;
import chappie.profile.impl.*;
import chappie.util.*;

public class Chaperone implements Runnable {
  // used for tracking instances; in reality, this is a singleton class since
  // there is no reason to have a second chaperone. even in the case of
  // parallel sampling, the singleton should manage the system.
  private static int id = 0;

  private int timerRate;
  private int epoch = 0;

  public Thread thread;
  private Logger logger;

  private ArrayList<Profiler> profilers = new ArrayList<Profiler>();

  public Chaperone() {
    logger = ChappieLogger.getLogger();
    logger.info("creating chappie instance " + id);

    // check if we are in nop
    timerRate = Integer.parseInt(System.getProperty("chappie.rate", "1"));
    if (timerRate > 0) {
      // setup the various profilers
      int vmRate = Integer.parseInt(System.getProperty("chappie.vm", "1"));
      if (vmRate > 0)
        profilers.add(new VMProfiler(vmRate, timerRate));

      int osRate = Integer.parseInt(System.getProperty("chappie.os", "1"));
      if (osRate > 0)
        profilers.add(new OSProfiler(osRate, timerRate));

      int raplRate = Integer.parseInt(System.getProperty("chappie.rapl", "1"));
      if (raplRate > 0)
        profilers.add(new RAPLProfiler(raplRate, timerRate));

      thread = new Thread(this, "chappie-" + id++);
    } else {
      // we probably need a runtime profiler to collect nop stats
      logger.info("running in nop mode");
    }
  }

  // Thread-like interface since we don't deal with the structure like a
  // runnable; I should find out if there's a better pattern
  public void start() {
    if (timerRate > 0)
      thread.start();
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
    }

    try {
      dump();
    } catch(IOException io) {
      logger.info("couldn't write observations: " + io.getMessage());
    }
  }

  // measurement of chappie's activeness for each epoch
  private class ChappieRecord extends Record {
    private long timestamp;
    private long elapsed;
    private long total;

    public ChappieRecord(int epoch, long timestamp, long elapsed, long total) {
      this.epoch = epoch;
      this.timestamp = timestamp;
      this.elapsed = elapsed;
      this.total = total;
    }

    protected String stringImpl() {
      return Integer.toString(epoch) + ";" +
        Long.toString(timestamp) + ";" +
        Long.toString(elapsed) + ";" +
        Long.toString(total);
    }

    private String[] header = new String[] { "epoch", "timestamp", "elapsed", "total" };
    public String[] headerImpl() {
      return header;
    };
  }

  private ArrayList<Record> data = new ArrayList<Record>();
  public void run() {
    while (!thread.interrupted()) {
      // in addition to sampling all profilers, chappie needs to
      // know when the samples are taken, how long the sampling took,
      // and how long the entire epoch took
      long timestamp = System.currentTimeMillis();
      long start = System.nanoTime();

      epoch++;
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
      data.add(new ChappieRecord(epoch, timestamp, elapsed, total));
    }
  }

  private void dump() throws IOException {
    logger.info("writing chappie data");
    chappie.util.CSV.write(data, "data/chappie.csv");

    for (Profiler profiler: profilers)
      profiler.dump();

    logger.info("done writing data");
  }
}
