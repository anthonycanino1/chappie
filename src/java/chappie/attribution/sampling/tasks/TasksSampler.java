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

import chappie.util.profiling.Sample;
import chappie.util.profiling.Sampler;
import java.util.HashMap;
import jlibc.proc.Task;

/** Sampler for task jiffies so any users can do relative tracking. */
public final class TasksSampler implements Sampler {
  private static HashMap<Integer, Task> sampleTasks() {
    HashMap<Integer, Task> tasks = new HashMap<>();
    for (Task task: Task.getAllTasks()) {
      tasks.put(task.getId(), task);
    }
    return tasks;
  }

  private HashMap<Integer, Task> last = sampleTasks();

  public TasksSampler() { }

  /** Returns the jiffies consumed by all tasks since the last sample. */
  @Override
  public Sample sample() {
    HashMap<Integer, Task> current = sampleTasks();
    TasksSample sample = new TasksSample(last, current);
    for (Task task: current.values()) {
      last.put(task.getId(), task);
    }
    return sample;
  }
}
