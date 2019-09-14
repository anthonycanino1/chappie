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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import chappie.glibc.GLIBC;

public class OSProfiler extends Profiler {
  File root = new File("/proc/" + GLIBC.getProcessId() + "/task/");
  public OSProfiler(int rate, int time) {
    super(rate, time);
  }

  private class ProcessRecord implements Record {
    private Integer epoch;
    private String stat;

    public ProcessRecord(int epoch, String stat) {
      this.epoch = epoch;
      this.stat = stat;
    }

    @Override
    public String toString() {
      return Stream.concat(
        Arrays.stream(new String[]{epoch.toString()}),
        Arrays.stream(GLIBC.parseProcessRecord(stat)))
        .collect(Collectors.joining(";"));
    }
  }

  private class SystemRecord implements Record {
    private Integer epoch;
    private ArrayList<String> stat;

    public SystemRecord(int epoch, ArrayList<String> stat) {
      this.epoch = epoch;
      this.stat = stat;
    }

    @Override
    public String toString() {
      return stat.stream()
        .map(GLIBC::parseSystemRecord)
        .map(Arrays::stream)
        .map(r -> Stream.concat(Arrays.stream(new String[]{epoch.toString()}), r))
        .map(r -> r.collect(Collectors.joining(";")))
        .map(Object::toString)
        .collect(Collectors.joining("\n"));
    }
  }

  ArrayList<Record> sysData = new ArrayList<Record>();
  public void sampleImpl(int epoch) {
    for (File f: root.listFiles())
      try {
        data.add(new ProcessRecord(epoch, GLIBC.readProcess(f.getName())));
      } catch (IOException io1) {
        logger.info("could not read " + f.getName() + ": " + io1.getMessage());
      }

    try {
      sysData.add(new SystemRecord(epoch, GLIBC.readSystem()));
    } catch (IOException io2) {
      logger.info("could not read system stats: " + io2.getMessage());
    }
  }

  private static String[] header = new String[] { "epoch", "id", "core", "state", "user", "system" };
  private static String[] sysHeader = new String[] { "epoch", "cpu", "user", "nice", "system", "idle", "iowait", "irq", "softirq", "steal", "guest", "guest_nice" };
  public void dump() throws IOException {
    logger.info("writing os data");

    chappie.util.CSV.write(data, "data/os.csv", header);
    chappie.util.CSV.write(sysData, "data/sys.csv", sysHeader);

    GLIBC.dumpMapping();
  }
}
