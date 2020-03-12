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

import jrapl.EnergyCheckUtils;

import chappie.Chaperone;
import chappie.profile.*;
import chappie.profile.util.*;

public class RAPLProfiler extends Profiler {
  // Since jRAPL returns a double[] from getEnergyStats, I wrote a helper to
  // reshape it into a socket x 4 array. This primarily helps with clean code
  // but could (should?) be in jRAPL instead.
  private static boolean noRapl = false;

  public RAPLProfiler(int rate, int time, String workDirectory) {
    super(rate, time, workDirectory);

    if (!noRapl) {
      try {
        sampleEnergy();
      } catch(Exception e) {
        noRapl = true;
        logger.info("no rapl available");
      }
    }
  }

  private static class EnergyRecord extends Record {
    private int socket;

    private double cpu;
    private double dram;

    private EnergyRecord(int epoch, double[] record) {
      this.epoch = epoch;
      this.socket = (int)record[0];
      this.cpu = record[3];
      this.dram = record[1];
    }

    protected String stringImpl() {
      return Integer.toString(socket) + ";" +
        Double.toString(cpu) + ";" +
        Double.toString(dram);
    }

    private static final String[] header = new String[] { "epoch", "socket", "package", "dram" };
    public static String[] getHeader() {
      return header;
    };
  }

  protected void sampleImpl(int epoch) {
    if (!noRapl)
      for (double[] record: sampleEnergy())
        data.add(new EnergyRecord(epoch, record));
  }

  private static double[][] sampleEnergy() {
    double[] energy = EnergyCheckUtils.getEnergyStats();
    double[][] parsedEnergy = new double[EnergyCheckUtils.socketNum][4];
    for (int i = 0; i < EnergyCheckUtils.socketNum; ++i) {
      parsedEnergy[i][0] = i;
      parsedEnergy[i][1] = energy[3 * i];
      parsedEnergy[i][2] = energy[3 * i + 1];
      parsedEnergy[i][3] = energy[3 * i + 2];
    }

    return parsedEnergy;
  }

  public void dumpImpl() throws IOException {
    if (!noRapl)
      CSV.write(data, EnergyRecord.getHeader(), Chaperone.getWorkDirectory() + "/energy.csv");
  }
}
