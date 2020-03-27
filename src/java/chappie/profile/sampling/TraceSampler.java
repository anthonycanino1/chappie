// /* ************************************************************************************************
//  * Permission is hereby granted, free of charge, to any person obtaining a copy of this
//  * Copyright 2019 SUNY Binghamton
//  * software and associated documentation files (the "Software"), to deal in the Software
//  * without restriction, including without limitation the rights to use, copy, modify, merge,
//  * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
//  * persons to whom the Software is furnished to do so, subject to the following conditions:
//  *
//  * The above copyright notice and this permission notice shall be included in all copies or
//  * substantial portions of the Software.
//  *
//  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
//  * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
//  * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
//  * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
//  * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
//  * DEALINGS IN THE SOFTWARE.
//  * ***********************************************************************************************/

package chappie.profile.sampling;

import chappie.profile.Record;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import one.profiler.AsyncProfiler;
import one.profiler.Events;

public final class TraceSampler {
  private static int asyncRate = 1000000 * Integer.parseInt(System.getProperty("chappie.async_rate", "1"));
  static {
    String chappieRoot = System.getProperty("user.dir");
    AsyncProfiler.getInstance(chappieRoot + "/build/libasyncProfiler.so").start(Events.CPU, asyncRate);
  }

  public static TraceRecordSet sample() {
    ArrayList<TraceRecord> traces = new ArrayList<>();
    AsyncProfiler.getInstance().stop();
    for (String trace: AsyncProfiler.getInstance().dumpRecords().split("\n"))
      if (trace.length() > 0)
        traces.add(new TraceRecord(trace));

    AsyncProfiler.getInstance().resume(Events.CPU, asyncRate);
    return new TraceRecordSet(traces);
  }

  public static class TraceRecord implements Record {
    private final long timestamp;
    private final int id;
    private final String trace;

    private TraceRecord(String frame) {
      String[] record = frame.split(";");
      this.timestamp = Long.parseLong(record[0]);
      this.id = Integer.parseInt(record[1]);
      this.trace = record[2];
    }

    public String getStackTrace() {
      return trace;
    }
  }

  public static class TraceRecordSet implements Record {
    private final HashMap<Integer, TraceRecord> records = new HashMap<>();

    private TraceRecordSet(ArrayList<TraceRecord> traces) {
      for (TraceRecord trace: traces) {
        records.put(trace.id, trace);
      }
    }

    public TraceRecord getTraceRecord(int tid) {
      return records.get(tid);
    }

    public Set<Integer> getTaskIds() {
      return records.keySet();
    }
  }
}
