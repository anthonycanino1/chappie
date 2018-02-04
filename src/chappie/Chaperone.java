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

  protected Map<Integer, List<Set<String>>> activity = new TreeMap<Integer, List<Set<String>>>();
  protected Map<String, Map<Integer, List<Double>>> power = new HashMap<String, Map<Integer, List<Double>>>();
  protected Map<String, Map<Integer, Integer>> cores = new HashMap<String, Map<Integer, Integer>>();
  protected Map<String, Map<Integer, Long>> memory = new HashMap<String, Map<Integer, Long>>();

  public abstract void run();

  public abstract int assign();
  public abstract List<Double> dismiss(int id);

  public void retire() {
    String activityName = System.getenv("CHAPPIE_ACTIVITY_LOG");
    if (activityName == null) {
      activityName = "chappie.activity.log";
    }

    String powerName = System.getenv("CHAPPIE_POWER_LOG");
    if (powerName == null) {
      powerName = "chappie.power.log";
    }

    String coreName = System.getenv("CHAPPIE_CORE_LOG");
    if (coreName == null) {
      coreName = "chappie.core.log";
    }

    String memoryName = System.getenv("CHAPPIE_MEMORY_LOG");
    if (memoryName == null) {
      memoryName = "chappie.memory.log";
    }

    PrintWriter log = null;

    try {
      log = new PrintWriter(new BufferedWriter(new FileWriter(activityName)));
    } catch (IOException io) {
      System.err.println("Error: " + io.getMessage());
      throw new RuntimeException("uh oh");
    }

    for (Integer time : activity.keySet()) {
      String message = time + ": (";
      for (String name : activity.get(time).get(0))
        message += name + ", ";

      message = message.substring(0, message.length() - 2) + "), (";
      for (String name : activity.get(time).get(1))
        message += name + ", ";

      message = message.substring(0, message.length() - 2) + ")";
      log.write(message + "\n");
    }

    log.close();

    try {
      log = new PrintWriter(new BufferedWriter(new FileWriter(powerName)));
    } catch (IOException io) {
      System.err.println("Error: " + io.getMessage());
      throw new RuntimeException("uh oh");
    }

    for (String name : power.keySet())
      for (Integer time : power.get(name).keySet()) {
        String message = name + ", " + time + ": (";
        for (double reading : power.get(name).get(time))
          message += reading + ", ";

        message = message.substring(0, message.length() - 2) + ")";
        log.write(message + "\n");
      }

    log.close();

    try {
      log = new PrintWriter(new BufferedWriter(new FileWriter(coreName)));
    } catch (IOException io) {
      System.err.println("Error: " + io.getMessage());
      throw new RuntimeException("uh oh");
    }

    for (String name : cores.keySet())
      for (Integer time : cores.get(name).keySet()) {
        String message = name + ", " + time + ": (" + cores.get(name).get(time) + ")";

        log.write(message + "\n");
    }

    log.close();

    try {
      log = new PrintWriter(new BufferedWriter(new FileWriter(memoryName)));
    } catch (IOException io) {
      System.err.println("Error: " + io.getMessage());
      throw new RuntimeException("uh oh");
    }

    for (String name : memory.keySet())
      for (Integer time : memory.get(name).keySet()) {
        String message = name + ", " + time + ": (" + memory.get(name).get(time) + ")";

        log.write(message + "\n");
    }

    log.close();
  }
}
