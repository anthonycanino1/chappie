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
import chappie.util.profiling.Sampler;
import java.util.ArrayList;
import java.util.HashMap;
import one.profiler.AsyncProfiler;
import one.profiler.Events;

/**
* Due to the nature of the async-profiler, we cannot build a relative sampler.
* All data collected needs to be expunged and placed elsewhere rapidly. Some
* work is still needed to make this async safe.
*/
public final class StackTraceSampler implements Sampler {
  private static int asyncRate = 1000000 * Integer.parseInt(System.getProperty("chappie.async_rate", "1"));
  private static final boolean noAsync = init();

  /** Set up the async-profiler. */
  // TODO(timur): this is pretty crappy; it only works if you ran from where chappie's jar is
  private static boolean init() {
    try {
      String chappieRoot = System.getProperty("user.dir");
      AsyncProfiler.getInstance(chappieRoot + "/build/libasyncProfiler.so").start(Events.CPU, asyncRate);
      System.out.println("started async-profiler at " + asyncRate + "ns");
      return false;
    } catch (Exception e) {
      System.out.println("unable to start async-profiler:");
      e.printStackTrace();
      return true;
    }
  }

  /**
  * Returns the records dumped from the async-profiler as a stack trace sample
  * set. The original data is delimited as id;ts;stck\nid;ts;stck, so our other
  * data structures will handle them as needed.
  */
  private static ArrayList<StackTraceSample> sampleStackTraces() {
    ArrayList<StackTraceSample> traces = new ArrayList<>();
    if (!noAsync) {
      AsyncProfiler.getInstance().stop();
      for (String trace: AsyncProfiler.getInstance().dumpRecords().split("\n")) {
        if (trace.length() > 0) {
          traces.add(new StackTraceSample(trace));
        }
      }
      AsyncProfiler.getInstance().resume(Events.CPU, asyncRate);
    }
    return traces;
  }

  public StackTraceSampler() { }

  public Sample sample() {
    return new StackTraceSampleSet(sampleStackTraces());
  }
}
