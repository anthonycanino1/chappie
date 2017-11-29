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

public abstract class Chaperone {
  protected Map<Integer, Set<String>> timeLine = new TreeMap<Integer, Set<String>>();
  protected Map<String, Map<Integer, List<Double>>> threadReadings = new HashMap<String, Map<Integer, List<Double>>>();
  protected Map<String, Map<Integer, BitSet>> threadAffinities = new HashMap<String, Map<Integer, BitSet>>();
  protected Map<Integer, List<Double>> readings = new TreeMap<Integer, List<Double>>();
  protected Map<Integer, List<Integer>> threadCount = new TreeMap<Integer, List<Integer>>();

  public abstract int assign();
  public abstract double[] dismiss(int id);

  public void retire() {
    String timelogname = System.getenv("CHAPPIE_TIME_LOG");
    if (timelogname == null) {
      timelogname = "chappie.time.log";
    }

    String readinglogname = System.getenv("CHAPPIE_READING_LOG");
    if (readinglogname == null) {
      readinglogname = "chappie.reading.log";
    }

    String powerlogname = System.getenv("CHAPPIE_POWER_LOG");
    if (powerlogname == null) {
      powerlogname = "chappie.power.log";
    }

    String countlogname = System.getenv("CHAPPIE_COUNT_LOG");
    if (countlogname == null) {
      countlogname = "chappie.count.log";
    }

    String affinitylogname = System.getenv("CHAPPIE_AFFINITY_LOG");
    if (affinitylogname == null) {
      affinitylogname = "chappie.affinity.log";
    }

    PrintWriter log = null;
    try {
      log = new PrintWriter(new BufferedWriter(new FileWriter(timelogname)));
    } catch (IOException io) {
      System.err.println("Error: " + io.getMessage());
      throw new RuntimeException("uh oh");
    }

    for (Integer time : timeLine.keySet()) {
      String message = time + ": (";
      for (String name : timeLine.get(time))
        message += name + ", ";

      message = message.substring(0, message.length() - 2) + ")";
      log.write(message + "\n");
    }

    log.close();

    try {
      log = new PrintWriter(new BufferedWriter(new FileWriter(readinglogname)));
    } catch (IOException io) {
      System.err.println("Error: " + io.getMessage());
      throw new RuntimeException("uh oh");
    }

    for (String name : threadReadings.keySet())
      for (Integer time : threadReadings.get(name).keySet()) {
        String message = name + ", " + time + ": (";
        for (double reading : threadReadings.get(name).get(time))
          message += reading + ", ";

        message = message.substring(0, message.length() - 2) + ")";
        log.write(message + "\n");
      }

    log.close();

    try {
      log = new PrintWriter(new BufferedWriter(new FileWriter(powerlogname)));
    } catch (IOException io) {
      System.err.println("Error: " + io.getMessage());
      throw new RuntimeException("uh oh");
    }

    for (Integer time : readings.keySet()) {
      String message = time + ": (";
      for (double reading : readings.get(time))
        message += reading + ", ";

      message = message.substring(0, message.length() - 2) + ")";
      log.write(message + "\n");
    }

    log.close();

    try {
      log = new PrintWriter(new BufferedWriter(new FileWriter(countlogname)));
    } catch (IOException io) {
      System.err.println("Error: " + io.getMessage());
      throw new RuntimeException("uh oh");
    }

    for (Integer time : threadCount.keySet()) {
      String message = time + ": (";
      for (int count : threadCount.get(time))
        message += count + ", ";

      message = message.substring(0, message.length() - 2) + ")";
      log.write(message + "\n");
    }

    log.close();

    try {
      log = new PrintWriter(new BufferedWriter(new FileWriter(affinitylogname)));
    } catch (IOException io) {
      System.err.println("Error: " + io.getMessage());
      throw new RuntimeException("uh oh");
    }

    for (String name : threadAffinities.keySet()) {
      for (Integer time : threadAffinities.get(name).keySet()) {
        String message = name + ", " + time + ": (" + threadAffinities.get(name).get(time).toString() + ")";
        log.write(message + "\n");
      }
    }

    log.close();
  }
}
