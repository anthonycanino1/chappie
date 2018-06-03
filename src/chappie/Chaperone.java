/* ************************************************************************************************
 * Copyright 2017 SUNY Binghamton
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
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

package chappie;

import java.util.List;

import java.util.Set;

import java.util.Map;
import java.util.TreeMap;
import java.util.HashMap;

import java.io.*;

public abstract class Chaperone implements Runnable {
  public static Chaperone chaperone;

  protected Map<Integer, List<Set<String>>> activity = new TreeMap<Integer, List<Set<String>>>();
  protected Map<String, Map<Integer, List<Double>>> power = new HashMap<String, Map<Integer, List<Double>>>();
  protected Map<String, Map<Integer, Integer>> cores = new HashMap<String, Map<Integer, Integer>>();
  protected Map<String, Map<Integer, Long>> bytes = new HashMap<String, Map<Integer, Long>>();

  public abstract void run();

  protected void retire() {
    PrintWriter log = null;

    String path = System.getenv("CHAPPIE_TRACE_LOG");
    if (path == null)
      path = "chappie.trace.csv";

    try {
      log = new PrintWriter(new BufferedWriter(new FileWriter(path)));
    } catch (IOException io) {
      System.err.println("Error: " + io.getMessage());
      throw new RuntimeException("uh oh");
    }

    String message = "time,count,state\n";
    log.write(message);
    for (Integer time : activity.keySet()) {
      message = time + "," + activity.get(time).get(0).size() + "," + "active\n";
      message += time + "," + activity.get(time).get(1).size() + "," + "inactive\n";
      log.write(message);
    }
    log.close();

    path = System.getenv("CHAPPIE_THREAD_LOG");
    if (path == null)
      path = "chappie.thread.csv";

    try {
      log = new PrintWriter(new BufferedWriter(new FileWriter(path)));
    } catch (IOException io) {
      System.err.println("Error: " + io.getMessage());
      throw new RuntimeException("uh oh");
    }

    message = "time,thread,core,package,dram,bytes\n";
    log.write(message);
    for (String name : power.keySet())
      for (Integer time : power.get(name).keySet()) {
        message = time + "," + name + "," + cores.get(name).get(time);
        message += "," + power.get(name).get(time).get(2) + "," + power.get(name).get(time).get(0);
        message += "," + bytes.get(name).get(time) + "\n";
        log.write(message);
      }

    log.close();
  }
}
