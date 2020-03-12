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

package chappie.profile.impl;

import java.io.IOException;
import java.util.ArrayList;

import one.profiler.AsyncProfiler;
import one.profiler.Events;

import chappie.Chaperone;
import chappie.profile.*;
import chappie.profile.util.*;

public class TraceProfiler extends Profiler {
  int asyncRate;
  int time;
  public TraceProfiler(int rate, int time, String workDirectory) {
    super(10, time, workDirectory);
    asyncRate = rate;
    logger.info("async-profiler set to " + (asyncRate * time / 1000) + "us");
    AsyncProfiler.getInstance("/home/timur/projects/chappie-dev/build/libasyncProfiler.so").start(Events.CPU, asyncRate * time);

    this.time = time;
  }

  private static class TraceRecord extends Record {
    private long timestamp;
    private int id;
    private String[] trace;

    private TraceRecord(int epoch, String frame) {
      this.epoch = epoch;
      String[] record = frame.split(";");
      this.timestamp = Long.parseLong(record[0]);
      this.id = Integer.parseInt(record[1]);
      this.trace = record[2].split("@");
    }

    protected String stringImpl() {
      String message = timestamp + ";" + id + ";";
      for (String method: trace)
        message += method + "@";

      message = message.substring(0, message.length() - 1);

      return message;
    }

    private static final String[] header = new String[] { "epoch", "timestamp", "id", "trace" };
    public static String[] getHeader() {
      return header;
    };
  }

  protected void sampleImpl(int epoch) {
    AsyncProfiler.getInstance().stop();
    for (String record: AsyncProfiler.getInstance().dumpRecords().split("\n"))
      if (record.length() > 0)
        data.add(new TraceRecord(epoch, record));

    AsyncProfiler.getInstance().resume(Events.CPU, asyncRate * time);
  }

  public void dumpImpl() throws IOException {
    AsyncProfiler.getInstance().stop();
    for (String record: AsyncProfiler.getInstance().dumpRecords().split("\n"))
      if (record.length() > 0)
        data.add(new TraceRecord(-1, record));

    CSV.write(data, TraceRecord.getHeader(), Chaperone.getWorkDirectory() + "/method.csv");
  }
}
