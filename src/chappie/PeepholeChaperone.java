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

import jrapl.EnergyCheckUtils;

public class PeepholeChaperone extends Chaperone {

  private int pid = -1;
  private Thread thread;

  public PeepholeChaperone() {
    pid = GLIBC.getProcessId();

    thread = new Thread(this, "Chaperone");
    thread.start();
  }

  private int assigned = 0;
  private int current = 0;

  private long polling = 5;
  private boolean running = true;

  public void run() {
    long start = System.currentTimeMillis();
    while(running) {
      if(assigned > 0)
        synchronized(activity) {
          Set<Thread> threadSet = Thread.getAllStackTraces().keySet();

          int stamp = (int)(System.currentTimeMillis() - start);
          current = stamp - (int)(stamp % polling);

          //current = 5 * activity.size();
          activity.put(current, new ArrayList<Set<String>>());
          activity.get(current).add(new HashSet<String>());
          activity.get(current).add(new HashSet<String>());

          for(Thread thread : threadSet) {
            if (thread.getState() == Thread.State.RUNNABLE)
              activity.get(current).get(0).add(thread.getName());
            else
              activity.get(current).get(1).add(thread.getName());
          }
        }

      try {
        Thread.sleep(polling);
      } catch (InterruptedException e) { Thread.currentThread().interrupt();}
    }

    return;
  }

  private double getUsage(String name, int start, int end) {
    int count = 0;
    synchronized(activity) {
      for(int i = start; i <= end; i += polling)
        if(activity.get(i).get(0).contains(name))
          count++;
    }

    return count / (double)(end - start + 1);
  }

  public synchronized int assign() {
    assigned++;

    String name = Thread.currentThread().getName();

    //archivePower(name);

    archiveCore(name);
    cores.get(name).put(current, lastCore.get(name));

    return current;
  }

  public synchronized List<Double> dismiss(int stamp) {
    assigned--;

    String name = Thread.currentThread().getName();

    /*archivePower(name);
    List<Double> measure = attributePower(name, stamp, current);
    power.get(name).put(stamp, measure);

    return measure;*/
    return new ArrayList<Double>();
  }

  public void retire() {
    running = false;
    try {
      thread.join();
    } catch (InterruptedException e) { }



    super.retire();
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
  private Map<String, Integer> ticks = new HashMap<String, Integer>();

  protected void archiveCore(String name) {
    if (!cores.containsKey(name)) {
      cores.put(name, new TreeMap<Integer, Integer>());
      lastCore.put(name, -1);
      ticks.put(name, 0);
    }

    if((ticks.get(name) % 20) == 0) {
      int tid = GLIBC.getThreadId();
      int core = -1;
      String path = "/proc/" + pid + "/task/" + tid + "/stat";

      Runtime r = Runtime.getRuntime();

      try {
        Process p = r.exec("cat " + path);
        InputStream in = p.getInputStream();
        BufferedInputStream buf = new BufferedInputStream(in);
        InputStreamReader inread = new InputStreamReader(buf);
        BufferedReader bufferedreader = new BufferedReader(inread);

        // get the core
        String line;
        while ((line = bufferedreader.readLine()) != null) {
          core = Integer.parseInt(line.split(" ")[38]);
          lastCore.put(name, core);
        }
        // Check for failure
        try {
          if (p.waitFor() != 0) {
            System.err.println("exit value = " + p.exitValue());
          }
        } catch (InterruptedException e) {
          System.err.println(e);
        } finally {
          // Close the InputStream
          bufferedreader.close();
          inread.close();
          buf.close();
          in.close();
        }
      } catch (IOException e) {
        System.err.println(e.getMessage());
      }
    }

    ticks.put(name, ticks.get(name) + 1);
  }
}
