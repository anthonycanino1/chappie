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

package chappie.attribution.sampling.trace;

import java.util.ArrayList;
import one.profiler.AsyncProfiler;
import one.profiler.Events;

/* Snapshot of a stack trace sampled from the async-profiler. */
public final class StackTraceSample {
  private final long timestamp;
  private final int id;
  private final StackTrace trace;

  StackTraceSample(String sample) {
    String[] record = sample.split(",");
    this.timestamp = Long.parseLong(record[0]);
    this.id = Integer.parseInt(record[1]);
    this.trace = new StackTrace(record[2]);
  }

  public long getTimestamp() {
    return timestamp;
  }

  public int getThreadId() {
    return id;
  }

  public StackTrace getStackTrace() {
    return trace;
  }

  @Override
  public String toString() {
    return String.join(",",
      Long.toString(timestamp),
      Integer.toString(id),
      trace.toString());
  }
}
