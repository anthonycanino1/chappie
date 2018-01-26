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

//import jrapl.EnergyCheckUtils;

public class GlobalChaperone extends Chaperone {

  private int pid = -1;
  private Thread thread;

  public GlobalChaperone() {
    pid = GLIBC.getProcessId();

    thread = new Thread(this, "Chaperone");
    thread.start();
  }

  private int curr = 0;

  private long polling = 5;
  private boolean running = false;

  public int assign() { running = true; return 0; }
  public List<Double> dismiss(int stamp) { running = false; return null; }

  public void run() {
    int pid = GLIBC.getProcessId();
    long start = System.currentTimeMillis();
    List<Double> previous = new ArrayList<Double>();
    //for (double value: EnergyCheckUtils.getEnergyStats())
      //previous.add(value);

    while(!running) {}

    while(running) {
      Set<Thread> threadSet = Thread.getAllStackTraces().keySet();

      int stamp = (int)(System.currentTimeMillis() - start);
      curr = stamp - (int)(stamp % polling);

      activity.put(curr, new ArrayList<Set<String>>());
      activity.get(curr).add(new HashSet<String>());
      activity.get(curr).add(new HashSet<String>());

      int i = 0;
      List<Double> current = new ArrayList<Double>();
      /*for (double value: EnergyCheckUtils.getEnergyStats())
        current.add(value - previous.get(i++));*/

      for(Thread thread : threadSet) {
        String name = thread.getName();
        if (thread.getState() == Thread.State.RUNNABLE)
          activity.get(curr).get(0).add(name);
        else
          activity.get(curr).get(1).add(name);

        if (!cores.containsKey(name))
          cores.put(name, new TreeMap<Integer, Integer>());

        cores.get(name).put(curr, GLIBC.getCore(pid,GLIBC.getThreadId()));
      }

      int size = activity.get(curr).get(0).size();
      for(String name: activity.get(curr).get(0)) {
        List<Double> reading = new ArrayList<Double>();
        /*for (double value: EnergyCheckUtils.getEnergyStats())
          reading.add(current.get(i++) / size);*/

        if(!power.containsKey(name))
          power.put(name, new TreeMap<Integer, List<Double>>());
        power.get(name).put(curr, reading);
      }

      try {
        Thread.sleep(polling);
      } catch (InterruptedException e) { Thread.currentThread().interrupt();}
    }

    return;
  }

  public void retire() {
    running = false;
    try {
      thread.join();
    } catch (InterruptedException e) { }

    super.retire();
  }
}
