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

public class VMProfiler {
  Config config;

  ThreadGroup rootGroup;

  public VMProfiler(Config config) {
    this.config = config;

    rootGroup = Thread.currentThread().getThreadGroup();
    ThreadGroup parentGroup;
    while ((parentGroup = rootGroup.getParent()) != null)
        rootGroup = parentGroup;
  }

  private ArrayList<ArrayList<Object>> data = new ArrayList<ArrayList<Object>>();

  public void sample(int epoch, long unixTime) {
    ArrayList<Object> record;

    // read vm state
    if (config.vmFactor > 0 && epoch % config.vmFactor == 0) {
      Thread[] threads = new Thread[rootGroup.activeCount()];
      while (rootGroup.enumerate(threads, true) == threads.length)
          threads = new Thread[threads.length * 2];

      for (Thread thread: threads)
        if (config.mode == Mode.SAMPLE && thread != null) {
          record = new ArrayList<Object>();

          record.add(epoch);
          record.add(unixTime);
          record.add(thread.getName());
          record.add(thread.getId());
          record.add(thread.getState());

          data.add(record);
        }
    }
  }

  public void dump() {
    if (mode == Mode.SAMPLE) {
      CSVPrinter printer = new CSVPrinter(
        new FileWriter(config.workDirectory + "/chappie.vm" + config.suffix + ".csv"),
        CSVFormat.DEFAULT.withHeader("epoch", "timestamp", "thread", "id", "state").withDelimiter(";")
      );

      printer.printRecords(data);
      printer.close();

      GLIBC.dumpTidMap();
    }
  }
}
