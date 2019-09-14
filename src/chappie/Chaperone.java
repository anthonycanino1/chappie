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
import chappie.profile.Profiler.Record;
import chappie.util.*;

public class Chaperone implements Runnable {
  static int id = 0;

  private int timerRate;
  private int epoch = 0;

  public Thread thread;
  private Logger logger;

  private ArrayList<Profiler> profilers = new ArrayList<Profiler>();

  public Chaperone() {
    logger = ChappieLogger.getLogger();
    logger.info("creating chappie instance " + id);

    timerRate = Integer.parseInt(System.getProperty("chappie.rate", "1"));
    if (timerRate > 0) {
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
      logger.info("running in nop mode");
    }
  }

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

  private class ChappieRecord implements Record {
    private Integer epoch;
    private long timestamp;
    private long elapsed;
    private long total;

    public ChappieRecord(int epoch, long timestamp, long elapsed, long total) {
      this.epoch = epoch;
      this.timestamp = timestamp;
      this.elapsed = elapsed;
      this.total = total;
    }

    @Override
    public String toString() {
      return Arrays.stream(new Object[]{epoch, timestamp, elapsed, total})
        .map(Object::toString)
        .collect(Collectors.joining(";"));
    }
  }

  private ArrayList<Record> data = new ArrayList<Record>();
  public void run() {
    while (!thread.interrupted()) {
      long timestamp = System.currentTimeMillis();
      long start = System.nanoTime();

      epoch++;
      for (Profiler profiler: profilers)
        profiler.sample(epoch);

      long elapsed = System.nanoTime() - start;

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

  private static String[] header = new String[] { "epoch", "timestamp", "elapsed", "total" };
  private void dump() throws IOException {
    logger.info("writing chappie data");
    chappie.util.CSV.write(data, "data/chappie.csv", header);

    for (Profiler profiler: profilers)
      profiler.dump();

    logger.info("done writing data");
  }
}
