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

public class OSProfiler extends Profiler {
  Task main = Task.mainTask();

  public OSProfiler(int rate, int time, String workDirectory) {
    super(rate, time, workDirectory);
  }

  private static class TaskRecord extends Record {
    private int id;
    private Task task;
    public TaskRecord(int epoch, Task task) {
      this.epoch = epoch;
      this.task = task;
    }

    public String stringImpl() {
      return Integer.toString(task.getId()) + ";" +
        Integer.toString(task.getCPU()) + ";" +
        task.getState().name() + ";" +
        Long.toString(task.getUserJiffies()) + ";" +
        Long.toString(task.getKernelJiffies());
    }

    private static final String[] header = new String[] { "epoch", "tid", "cpu", "state", "user", "sys" };
    public static String[] getHeader() {
      return header;
    };
  }

  private static class CPURecord extends Record {
    private int id;
    private CPU cpu;

    public CPURecord(int epoch, CPU cpu) {
      this.epoch = epoch;
      this.cpu = cpu;
    }

    public String stringImpl() {
      return cpu.getCPU() + ";" +
        cpu.getUserJiffies() + ";" +
        cpu.getNiceJiffies() + ";" +
        cpu.getKernelJiffies() + ";" +
        cpu.getIdleJiffies() + ";" +
        cpu.getIOWaitJiffies() + ";" +
        cpu.getIRQJiffies() + ";" +
        cpu.getSoftIRQJiffies() + ";" +
        cpu.getStealJiffies() + ";" +
        cpu.getGuestJiffies() + ";" +
        cpu.getGuestNiceJiffies();
    }

    private static final String[] header = new String[] {
      "epoch", "cpu",
      "user", "nice", "system", "idle", "iowait",
      "irq", "softirq", "steal", "guest", "guest_nice"
    };
    public static String[] getHeader() {
      return header;
    };
  }

  ArrayList<Record> sysData = new ArrayList<Record>();
  public void sampleImpl(int epoch) {
    for (Task task: main.getTasks())
      data.add(new TaskRecord(epoch, task));

    for (CPU cpu: CPU.getCPUs())
      sysData.add(new CPURecord(epoch, cpu));
  }

  public void dumpImpl() throws IOException {
    CSV.write(data, TaskRecord.getHeader(), Chaperone.getWorkDirectory() + "/task.csv");
    CSV.write(sysData, CPURecord.getHeader(), Chaperone.getWorkDirectory() + "/cpu.csv");

    JSON.write(Task.getTaskIds(), Chaperone.getWorkDirectory() + "/tid.json");
    JSON.write(Task.getTaskNames(), Chaperone.getWorkDirectory() + "/name.json");
  }
}
