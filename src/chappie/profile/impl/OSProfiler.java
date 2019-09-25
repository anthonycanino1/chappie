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
import chappie.glibc.*;
import chappie.profile.*;
import chappie.util.*;

public class OSProfiler extends Profiler {
  public OSProfiler(int rate, int time) {
    super(rate, time);
  }

  private class OSProcessRecord extends Record {
    private int id;
    private OSProcess proc;
    public OSProcessRecord(int epoch, OSProcess proc) {
      this.epoch = epoch;
      this.proc = proc;
    }

    public String stringImpl() {
      return proc.parse().toString();
    }

    private String[] header = new String[] { "epoch", "tid", "cpu", "state", "user", "sys" };
    public String[] headerImpl() {
      return header;
    };
  }

  private class CPURecord extends Record {
    private int id;
    private CPU cpu;

    public CPURecord(int epoch, CPU cpu) {
      this.epoch = epoch;
      this.cpu = cpu;
    }

    public String stringImpl() {
      return cpu.parse().toString();
    }

    private String[] header = new String[] {
      "epoch", "cpu",
      "user", "nice", "system", "idle", "iowait",
      "irq", "softirq", "steal", "guest", "guestNice"
    };
    public String[] headerImpl() {
      return header;
    };
  }

  ArrayList<Record> sysData = new ArrayList<Record>();
  public void sampleImpl(int epoch) {
    for (OSProcess proc: OSProcess.currentProcess().getTasks())
      data.add(new OSProcessRecord(epoch, proc));

    for (CPU cpu: CPU.getCPUs())
      sysData.add(new CPURecord(epoch, cpu));
  }

  public void dumpImpl() throws IOException {
    chappie.util.CSV.write(data, Chaperone.getWorkDirectory() + "/os.csv");
    chappie.util.CSV.write(sysData, Chaperone.getWorkDirectory() + "/sys.csv");

    GLIBC.dump();
    OSProcess.dump();
  }
}
