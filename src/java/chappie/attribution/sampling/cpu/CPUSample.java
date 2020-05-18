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

package chappie.attribution.sampling.cpu;

import static jrapl.util.EnergyCheckUtils.SOCKETS;

import chappie.util.profiling.Sample;
import java.lang.Math;
import java.time.Instant;
import java.util.Arrays;
import jlibc.proc.CPU;

/** Data structure for jiffies difference by socket between CPU snapshots. */
public final class CPUSample implements Sample {
  private static final int CORES = Runtime.getRuntime().availableProcessors();

  private final Instant timestamp;
  private final long[] jiffies = new long[SOCKETS];

  CPUSample(CPU[] first, CPU[] second) {
    timestamp = Instant.now();
    if (first.length == CORES && second.length == CORES) {
      for (int core = 0; core < CORES; core++) {
        long jiffies = second[core].getUserJiffies() - first[core].getUserJiffies();
        jiffies += second[core].getNiceJiffies() - first[core].getNiceJiffies();
        jiffies += second[core].getKernelJiffies() - first[core].getKernelJiffies();
        jiffies += second[core].getIOWaitJiffies() - first[core].getIOWaitJiffies();
        jiffies += second[core].getIRQJiffies() - first[core].getIRQJiffies();
        jiffies += second[core].getSoftIRQJiffies() - first[core].getSoftIRQJiffies();
        jiffies += second[core].getStealJiffies() - first[core].getStealJiffies();
        jiffies += second[core].getGuestJiffies() - first[core].getGuestJiffies();
        jiffies += second[core].getGuestNiceJiffies() - first[core].getGuestNiceJiffies();
        // TODO(timurbey): need a generic map
        this.jiffies[(int)(core / (CORES / SOCKETS))] += jiffies;
      }
    }
  }

  @Override
  public Sample merge(Sample other) {
    if (other instanceof CPUSample) {
      for (int socket = 0; socket < SOCKETS; socket++) {
        this.jiffies[socket] += ((CPUSample) other).jiffies[socket];
      }
    }
    return this;
  }

  @Override
  public Instant getTimestamp() {
    return timestamp;
  }

  @Override
  public String toString() {
    return String.join(",",
      Long.toString(timestamp.toEpochMilli()),
      String.join(",", Arrays.stream(jiffies).mapToObj(Long::toString).toArray(String[]::new)));
  }

  public long getJiffies(int socket) {
    return jiffies[socket];
  }
}
