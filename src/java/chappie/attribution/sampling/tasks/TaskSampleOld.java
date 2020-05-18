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

package chappie.attribution.sampling.task;

import jlibc.proc.Task;

/** Data structure for relative jiffies consumption of a task. */
class TaskSample {
  private static final int CORES = Runtime.getRuntime().availableProcessors();

  final long start;
  final long end;
  final int id;
  final int socket;
  final long userJiffies;
  final long kernelJiffies;

  TaskSample(Task first, Task second) {
    this.start = first.getTimestamp();
    this.end = second.getTimestamp();
    this.id = second.getId();
    // need to find a generic map from cpu to socket
    this.socket = second.getCPU() < 20 ? 0 : 1;
    this.userJiffies = second.getUserJiffies() - first.getUserJiffies();
    this.kernelJiffies = second.getKernelJiffies() - first.getKernelJiffies();
  }

  @Override
  public String toString() {
    return String.join(",",
      Long.toString(start),
      Long.toString(end),
      Integer.toString(id),
      Integer.toString(socket),
      Long.toString(userJiffies),
      Long.toString(kernelJiffies));
  }
}
