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
import java.util.Set;
import jlibc.proc.Task;
import jrapl.EnergyCheckUtils;

public final class TaskSampler {
  private static final Task main = Task.mainTask();

  private static HashMap<Integer, Task> sampleTasks() {
    HashMap<Integer, Task> records = new HashMap<>();
    for (Task task: main.getTasks()) {
      records.put(task.getId(), task);
    }
    return records;
  }

  private static final HashMap<Integer, Task> last = sampleTasks();

  public static TaskRecordSet sample() {
    HashMap<Integer, Task> current = sampleTasks();
    ArrayList<TaskRecord> tasks = new ArrayList<>();
    for (int id: current.keySet()) {
      if (last.containsKey(id)) {
        long userJiffies = current.get(id).getUserJiffies() - last.get(id).getUserJiffies();
        long kernelJiffies = current.get(id).getKernelJiffies() - last.get(id).getKernelJiffies();
        // if (userJiffies > 0 || kernelJiffies > 0) {
        tasks.add(new TaskRecord(current.get(id), userJiffies, kernelJiffies));
        // }
      }
      last.put(id, current.get(id));
    }

    return new TaskRecordSet(tasks);
  }

  private static class TaskRecord implements Record {
    private final int id;
    private final int socket;
    private final long userJiffies;
    private final long kernelJiffies;

    private TaskRecord(Task task, long userJiffies, long kernelJiffies) {
      this.id = task.getId();
      // need to find a generic map from cpu to socket
      this.socket = task.getCPU() < 20 ? 0 : 1;
      this.userJiffies = userJiffies;
      this.kernelJiffies = kernelJiffies;
    }

    @Override
    public String toString() {
      return "task id:" + id + ", socket:" + socket + ", user jiffies:" + userJiffies + ", kernel jiffies:" + kernelJiffies;
    }
  }

  public static class TaskRecordSet implements Record {
    private final HashMap<Integer, TaskRecord> records = new HashMap<>();
    private final long[] jiffies = new long[EnergyCheckUtils.socketNum];

    private TaskRecordSet(ArrayList<TaskRecord> tasks) {
      for (TaskRecord task: tasks) {
        records.put(task.id, task);
        jiffies[task.socket] += task.userJiffies + task.kernelJiffies;
      }
    }

    public TaskRecord getTaskRecord(int tid) {
      return records.get(tid);
    }

    public Set<Integer> getTaskIds() {
      return records.keySet();
    }

    public long getJiffies(int socket) {
      return jiffies[socket];
    }

    @Override
    public String toString() {
      String message = "jiffies: ";
      for (long jiff: jiffies) {
        message += "" + jiff + ", ";
      }

      return message + records.toString();
    }
  }
}
