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

package chappie.profile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import jrapl.EnergyCheckUtils;

import chappie.util.*;

public class RAPLProfiler extends Profiler {
  private static boolean noRapl = false;
  private EnergyRecord initReading;
  public RAPLProfiler(int rate, int time) {
    super(rate, time);

    if (!noRapl) {
      try {
        initReading = new EnergyRecord(-1, EnergyCheckUtils.getEnergyStats());
      } catch(Exception e) {
        noRapl = true;
        logger.info("no rapl available");
      }
    }
  }

  private static int sockets = EnergyCheckUtils.GetSocketNum();
  private class EnergyRecord implements Record {

    private Integer epoch;
    private double[][] reading;

    private EnergyRecord(int epoch, double[] reading) {
      this.epoch = epoch;

      this.reading = new double[RAPLProfiler.sockets][5];
      for (int i = 0; i < RAPLProfiler.sockets; i++) {
        this.reading[i][0] = epoch;
        this.reading[i][1] = i;
        for (int j = 0; j < 3; j++)
          this.reading[i][j + 2] = reading[3 * i + j];
      }
    }

    @Override
    public String toString() {
      return Arrays.stream(reading)
        .map(DoubleStream::of)
        .map(r -> r.boxed().map(Object::toString).collect(Collectors.joining(";")))
        .map(Object::toString)
        .collect(Collectors.joining("\n"));
    }
  }

  protected void sampleImpl(int epoch) {
    if (!noRapl)
      data.add(new EnergyRecord(epoch, EnergyCheckUtils.getEnergyStats()));
  }

  private static String[] header = new String[] { "epoch", "cpu", "uncore", "dram" };
  public void dump() throws IOException {
    if (!noRapl) {
      logger.info("writing energy data");

      chappie.util.CSV.write(data, "data/energy.csv", header);
    }
  }
}
