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

package chappie.attribution.sampling.energy;

import static jrapl.util.EnergyCheckUtils.SOCKETS;

import chappie.util.profiling.Sample;
import java.time.Instant;
import java.util.Arrays;
import jrapl.EnergyStats;

/** Collection of energy stats that can be accessed by socket. */
public final class EnergySample implements Sample {
  private final Instant timestamp;
  private final double[] energy = new double[SOCKETS];

  EnergySample(EnergyStats[] first, EnergyStats[] second) {
    timestamp = Instant.now();
    for (int socket = 0; socket < SOCKETS; socket++) {
      EnergyStats stats = second[socket].difference(first[socket]);
      energy[socket] = stats.getCpu() + stats.getPackage() + stats.getDram();
    }
  }

  @Override
  public Sample merge(Sample other) {
    if (other instanceof EnergySample) {
      for (int socket = 0; socket < SOCKETS; socket++) {
        this.energy[socket] += ((EnergySample) other).energy[socket];
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
      String.join(",", Arrays.stream(energy).mapToObj(Double::toString).toArray(String[]::new)));
  }

  public double getEnergy(int socket) {
    return energy[socket];
  }
}
