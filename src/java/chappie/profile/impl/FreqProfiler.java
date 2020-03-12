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

import chappie.Chaperone;
import chappie.profile.*;
import chappie.profile.util.*;

import jlibc.proc.*;

public class FreqProfiler extends Profiler {
  Task main = Task.mainTask();

  public FreqProfiler(int rate, int time, String workDirectory) {
    super(rate, time, workDirectory);
  }

  private static class FreqRecord extends Record {
    private int id;
    private int cpu;
    private long freq;
    public FreqRecord(int epoch, int cpu, long freq) {
      this.epoch = epoch;
      this.cpu = cpu;
      this.freq = freq;
    }

    public String stringImpl() {
      return Integer.toString(cpu) + ";" + Long.toString(freq);
    }

    private static final String[] header = new String[] { "epoch", "cpu", "freq" };
    public static String[] getHeader() {
      return header;
    };
  }

  public void sampleImpl(int epoch) {
    // for (long freq: CPU.getFreqs())
    for (int i = 0; i < 40; i++) {
      long freq = CPU.getFreq(i);
      data.add(new FreqRecord(epoch, i, freq));
    }
  }

  public void dumpImpl() throws IOException {
    CSV.write(data, FreqRecord.getHeader(), Chaperone.getWorkDirectory() + "/freqs.csv");
    JSON.write(CPU.getMaxFreqs(), Chaperone.getWorkDirectory() + "/freqs.json");
  }
}
