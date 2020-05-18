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

package chappie.attribution.sampling.tasks;

import static jrapl.util.EnergyCheckUtils.SOCKETS;

import chappie.util.profiling.Sample;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import jlibc.proc.Task;

/** Collection of relative task snapshots that can be accessed by socket. */
public class TasksSample implements Sample {
  private static final int CORES = Runtime.getRuntime().availableProcessors();

  private final Instant timestamp;
  private final int[] count = new int[SOCKETS];
  private final long[] jiffies = new long[SOCKETS];
  private final HashMap<Integer, Integer> taskSockets = new HashMap<>();

  TasksSample(Map<Integer, Task> first, Map<Integer, Task> second) {
    timestamp = Instant.now();
    for (int id: second.keySet()) {
      if (first.containsKey(id)) {
        // not right yet, we need to track counts of each task
        int socket = (int)(second.get(id).getCPU() / (CORES / SOCKETS));
        jiffies[socket] += second.get(id).getUserJiffies() - first.get(id).getUserJiffies();
        jiffies[socket] += second.get(id).getKernelJiffies() - first.get(id).getKernelJiffies();
        count[socket]++;
        taskSockets.put(id, socket);
      }
    }
  }

  @Override
  public Sample merge(Sample other) {
    if (other instanceof TasksSample) {
      for (int socket = 0; socket < SOCKETS; socket++) {
        this.count[socket] += ((TasksSample) other).count[socket];
        this.jiffies[socket] += ((TasksSample) other).jiffies[socket];
      }
      taskSockets.putAll(((TasksSample) other).taskSockets);
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
      String.join(",", Arrays.stream(jiffies).mapToObj(Long::toString).toArray(String[]::new)));
  }

  public long getJiffies(int socket) {
    return jiffies[socket];
  }

  public int getTaskCount(int socket) {
    return count[socket];
  }

  public int getSocket(int tid) {
    return taskSockets.get(tid);
  }

  public Set<Integer> getTasks() {
    return taskSockets.keySet();
  }
}
