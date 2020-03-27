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

package chappie.profile.sampling;

import chappie.profile.Record;
import java.util.ArrayList;
import java.util.HashMap;
import jrapl.EnergyCheckUtils;

public final class RAPLSampler {
  // Since jRAPL returns a double[] from getEnergyStats, I wrote a helper to
  // reshape it into a socket x 4 array. This primarily helps with clean code
  // but could (should?) be in jRAPL instead.
  private static boolean noRapl = false;

  private static EnergyRecord sampleEnergy() {
    try {
      double[] energy = EnergyCheckUtils.getEnergyStats();
      EnergyReading[] readings = new EnergyReading[EnergyCheckUtils.socketNum];
      for (int i = 0; i < EnergyCheckUtils.socketNum; ++i) {
        readings[i] = new EnergyReading(energy[3 * i + 2], energy[3 * i]);
      }

      return new EnergyRecord(readings);
    } catch (Exception e) {
      noRapl = true;

      return new EnergyRecord(new EnergyReading[0]);
    }
  }

  private static EnergyRecord last = sampleEnergy();

  public static EnergyRecord sample() {
    EnergyReading[] diff = new EnergyReading[EnergyCheckUtils.socketNum];
    if (!noRapl) {
      EnergyRecord current = sampleEnergy();
      for (int socket = 0; socket < EnergyCheckUtils.socketNum; ++socket) {
        double cpu = current.getReading(socket).getPackage() - last.getReading(socket).getPackage();
        if (cpu < 0) {
          cpu += EnergyCheckUtils.wraparoundValue;
        }

        double dram = current.getReading(socket).getDram() - last.getReading(socket).getDram();
        if (dram < 0) {
          dram += EnergyCheckUtils.wraparoundValue;
        }

        diff[socket] = new EnergyReading(cpu, dram);
      }
      last = current;
    }

    return new EnergyRecord(diff);
  }

  public static class EnergyReading implements Record {
    private final double cpu;
    private final double dram;

    private EnergyReading(double cpu, double dram) {
      this.cpu = cpu;
      this.dram = dram;
    }

    public double getPackage() {
      return cpu;
    }

    public double getDram() {
      return dram;
    }

    public double getEnergy() {
      return cpu + dram;
    }

    @Override
    public String toString() {
      return "package:" + cpu + ", dram:" + dram;
    }
  }

  public static class EnergyRecord implements Record {
    private final EnergyReading[] readings;

    private EnergyRecord(EnergyReading[] readings) {
      this.readings = readings;
    }

    @Override
    public String toString() {
      String message = "\n";
      for (int socket = 0; socket < EnergyCheckUtils.socketNum; socket++){
        message += "socket:" + socket + ", " + readings[socket] + "\n";
      }
      return message.substring(0, message.length() - 1);
    }

    public EnergyReading getReading(int socket) {
      return readings[socket];
    }
  }
}
