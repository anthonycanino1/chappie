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

import chappie.util.GLIBC;

import java.util.List;
import java.util.ArrayList;

import java.util.Set;
import java.util.HashSet;

import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;

import java.io.*;


import java.lang.management.*;
import com.sun.management.*;

import EnergyCheckUtils.*;

public class PeepholeChaperone extends Chaperone {

  private int pid = -1;
  private Thread thread;

  private long polling = 5;
  private boolean running = true;

  private com.sun.management.ThreadMXBean bean;

  public PeepholeChaperone() {
    pid = GLIBC.getProcessId();
    bean = (com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();

    thread = new Thread(this, "Chaperone");
    thread.start();
  }

  public PeepholeChaperone(int polling) {
    this.polling = polling;
    pid = GLIBC.getProcessId();

    thread = new Thread(this, "Chaperone");
    thread.start();
  }

  private int assigned = 0;
  private int current = 0;

  private Map<String, Integer> tids = new HashMap<String, Integer>();

  public void run() {
    long start = System.currentTimeMillis();
    while(running) {
      if(assigned > 0) {
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();

        int stamp = (int)(System.currentTimeMillis() - start);
        current = stamp - (int)(stamp % polling);

        activity.put(current, new ArrayList<Set<String>>());
        activity.get(current).add(new HashSet<String>());
        activity.get(current).add(new HashSet<String>());

        for(Thread thread : threadSet) {
          if (thread.getState() == Thread.State.RUNNABLE)
            activity.get(current).get(0).add(thread.getName());
          else
            activity.get(current).get(1).add(thread.getName());

          if (GLIBC.getState(pid, tids.get(thread.getName())) == "R")
            activity.get(current).get(0).add(thread.getName());
          else
            activity.get(current).get(1).add(thread.getName());
        }
      }

      try {
        Thread.sleep(polling);
      } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    return;
  }

  private double getUsage(String name, int start, int end) {
    int count = 0;
    int total = 0;
    synchronized(activity) {
      for(int i = start; i <= end; i += polling) {
        if(activity.get(i).get(0).contains(name))
          count++;

        total += activity.get(i).get(0).size();
      }
    }

    return count / (double)(end - start + 1) / total;
  }

  public synchronized int assign() {
    assigned++;

    String name = Thread.currentThread().getName();

    archiveTID(name);
    archivePower(name);
    archiveCore(name);

    cores.get(name).put(current, lastCore.get(name));

    return current;
  }

  public synchronized List<Double> dismiss(int previous) {
    assigned--;

    String name = Thread.currentThread().getName();

    archivePower(name);
    List<Double> measure = attributePower(name, previous, current);
    power.get(name).put(previous, measure);

    return measure;
  }

  public void retire() {
    running = false;
    try {
      thread.join();
    } catch (InterruptedException e) { }

    super.retire();
  }

  protected void archiveTID(String name) {
    if (!tids.containsKey(name))
      tids.put(name, GLIBC.getThreadId());
  }

  private Map<Integer, List<Double>> readings = new HashMap<Integer, List<Double>>();

  protected void archivePower(String name) {
    if (!power.containsKey(name))
      power.put(name, new TreeMap<Integer, List<Double>>());

    if(!readings.containsKey(current)) {
      List<Double> reading = new ArrayList<Double>();
      for (double value: EnergyCheckUtils.getEnergyStats())
        reading.add(value);

      readings.put(current, reading);
    }
  }

  protected List<Double> attributePower(String name, int first, int second) {
    double usage = getUsage(name, first, second);

    List<Double> m1 = readings.get(first);
    List<Double> m2 = readings.get(second);

    List<Double> reading = new ArrayList<Double>();
    for(int i = 0; i < m1.size(); ++i)
      reading.add(usage * (m2.get(i) - m1.get(i)));

    return reading;
  }

  private Map<String, Integer> lastCore = new HashMap<String, Integer>();

  protected void archiveCore(String name) {
    if (!cores.containsKey(name)) {
      cores.put(name, new TreeMap<Integer, Integer>());
      lastCore.put(name, -1);
    }

    lastCore.put(name, GLIBC.getCore(pid, GLIBC.getThreadId()));
  }

  private Map<String, Map<Integer, Long>> memory = new HashMap<String, Map<Integer, Long>>();

  protected void archiveMemory(String name) {
    if (!memory.containsKey(name))
      power.put(name, new TreeMap<Integer, List<Double>>());

    if(!memory.get(name).containsKey(current))
      memory.get(name).put(current, bean.getThreadAllocatedBytes(Thread.currentThread().getId()));
  }
}
