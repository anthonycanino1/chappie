/* ************************************************************************************************
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * Copyright 2017 SUNY Binghamton
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

import java.io.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import java.util.List;
import java.util.Map;

import chappie.Chaperone.Config;
import chappie.Chaperone.Mode;
import chappie.glibc.GLIBC;

import jrapl.EnergyCheckUtils.*;

public class RAPLProfiler {
  Config config;

  private double[] initialRaplReading;

  public RAPLProfiler(Config config) {
    this.config = config;

    initialRaplReading = jrapl.EnergyCheckUtils.getEnergyStats();
  }

  private ArrayList<ArrayList<Object>> data = new ArrayList<ArrayList<Object>>();

  public void sample(int epoch, long unixTime) {
    ArrayList<Object> record;

    // read energy
    if (config.mode == Mode.SAMPLE && config.raplFactor > 0 && epoch % config.raplFactor == 0) {
      double[] raplReading = jrapl.EnergyCheckUtils.getEnergyStats();

      for (int i = 0; i < raplReading.length / 3; ++i) {
        record = new ArrayList<Object>();

        record.add(epoch);
        record.add(unixTime);
        record.add(i + 1);
        record.add(raplReading[3 * i + 2]);
        record.add(raplReading[3 * i]);

        data.add(record);
      }
    }
  }

  public void dump() {
    if (mode == Mode.SAMPLE) {
      CSVPrinter printer = new CSVPrinter(
        new FileWriter(config.workDirectory + "/chappie.rapl" + config.suffix + ".csv"),
        CSVFormat.DEFAULT.withHeader("epoch", "timestamp", "socket", "package", "dram").withDelimiter(";")
      );

      printer.printRecords(data);
      printer.close();
    }
  }
}
