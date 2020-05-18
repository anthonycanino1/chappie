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

import chappie.util.profiling.Sample;
import java.lang.Iterable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Set;

/* Collection of stack trace samples. I don't remember why this is useful now. */
public final class StackTraceSampleSet implements Sample {
  private final HashMap<Integer, StackTraceSample> records = new HashMap<>();

  @Override
  public Instant getTimestamp() {
    return null;
  }

  StackTraceSampleSet(Iterable<StackTraceSample> samples) {
    for (StackTraceSample sample: samples) {
      records.put(sample.getThreadId(), sample);
    }
  }

  // how do we get timestamp mapping?
  public StackTrace getStackTrace(int tid) {
    return records.get(tid).getStackTrace();
  }

  public Set<Integer> getTaskIds() {
    return records.keySet();
  }

  @Override
  public Sample merge(Sample other) { return null; }

  // @Override
  // public long getTimestamp() {
  //   return 0;
  // }
}
