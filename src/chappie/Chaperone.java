/* ************************************************************************************************
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * Copyright 2017 SUNY Binghamton
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

import chappie.online.Attribution;
import chappie.online.OnlineTester;
import chappie.online.ThreadEnergyAttribution;
import chappie.util.*;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

import java.util.Set;
import java.util.HashSet;

import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import java.io.*;

import java.nio.file.Paths;
import java.nio.file.Files;

import java.lang.management.*;
import com.sun.management.*;

import jrapl.EnergyCheckUtils.*;

import java.lang.Thread;
import java.lang.reflect.Method;

import java.net.URL;
import java.net.URLClassLoader;

import java.util.Timer;
import java.util.TimerTask;

public class Chaperone extends TimerTask {


  public static int NUM_SOCKETS=2;
  public static final int VIRTUAL_CORES = 40;
  public static final int CORE_PER_SOCKETS = 20;
  public static final int NUMER_OF_SOCKETS = VIRTUAL_CORES/CORE_PER_SOCKETS;
  public static final int JIFF_LEN = 10;
  public static final int EPOCH_RATE=10;


  public enum Mode {NOP, SAMPLE}

  // Chaperone Parameters
  private Mode mode;


  private int polling;
  private int osReadingFactor;
  private int jraplReadingFactor;

  private boolean readMemory;
  private boolean readJiffies;

  // Metrics
  private int epoch = 0;
  private com.sun.management.ThreadMXBean bean;
  private long start;
  private long elapsedTime = 0;
  private long chappieReadingTime = 0;
  private boolean test_online_attribution;

  private Timer timer;
  Attribution attrib;
  private double dram_total = 0.0;
  private double package_total = 0.0;
  private int lastEpoch = 0;

  //private Map<Integer, List<ThreadEnergyAttribution>> energyMap = new HashMap<Integer, List<ThreadEnergyAttribution>>();
  private List<String> energyList= new ArrayList<String>();
  int epoch_len = 10;

  public Chaperone(Mode mode, int polling, int osRate, int jraplRate, boolean memory, boolean readJiffies,boolean test_online_attribution, int epoch_len) {
    this.mode = mode;

    this.polling = polling;
    this.osReadingFactor = osRate;
    this.jraplReadingFactor = jraplRate;

    this.readMemory = memory;
    this.readJiffies = readJiffies;
    this.test_online_attribution = test_online_attribution;
    this.epoch_len = epoch_len;

    this.epoch = 0;

    bean = (com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();

    start = System.nanoTime();

    if (mode != Mode.NOP) {
      timer = new Timer("Chaperone");
      timer.scheduleAtFixedRate(this, 0, polling);
    }

    attrib = new Attribution();
    attrib.init_attribution(this);


  }

  // Runtime data containers
  private List<List<Object>> application = new ArrayList<List<Object>>();
  private List<List<Object>> threads = new ArrayList<List<Object>>();
  private List<List<Object>> energy = new ArrayList<List<Object>>();
  private ArrayList<String> jiffies = new ArrayList<String>();


  public int get_current_epoch() {
    return epoch;
  }

  /**
   *
   * Will pull all system jiffies readings from epoch (epoch-1 ) up to epoch -n
   *
   *
   * @param start Start epoch to fetch
   * @param end Last epoch to fetch inclusive
   */
  public List<String> get_sys_jiffies(int start, int end) {
    return jiffies.subList(start,end+1);
  }

  public List<List<Object>> get_thread_info(int start, int end)   {

    int index_start = -1;
    int index_end = 0;
    for(int i=0; i<threads.size(); i++){
      int current = (int)(threads.get(i)).get(0);
      if(current == start && index_start == -1)
        index_start = i;
      if(current == end)
        index_end = i;

      if(current > end)
        break;
    }
    return threads.subList(index_start, index_end);
  }

  public List<List<Object>> get_energy_info(int start, int end) {
    return energy.subList(start*2, (end*2)+2);
  }

  /**
   *
   * Will pull all application jiffies readings from epoch (epoch - 1 - n ) up to epoch -1
   * int epoch, int n
   *
   * @param start Start epoch to fetch
   * @param end Last epoch to fetch inclusive
   */
  public List<List<Object>> application_jiffies(int start, int end) {
    //return application.subList(start,end+1);
    int index_start = -1;
    int index_end = 0;
    for(int i=0; i<threads.size(); i++){
      int current = (int)(threads.get(i)).get(0);
      if(current == start && index_start == -1)
        index_start = i;
      if(current == end)
        index_end = i;

      if(current > end)
        break;
    }
    return application.subList(index_start, index_end);

  }


  // Management for lazy/staggered sampling
  private int head = 0;
  private Set<Thread> threadSet = new HashSet<Thread>();

  private double[] lastRAPLReading = new double[0];

  // Termination flags
  // The timer class does not have a proper termination method. Luckily,
  // only main touches the chaperone, so we can use a double flag lock.
  private boolean terminate = false;
  private boolean terminated = false;


  @Override
  public void run() {
    // Check if we need to stop
    if (!terminate) {
      List<Object> measure;
      long unixTime = System.currentTimeMillis();

      long lastReading = elapsedTime;
      elapsedTime = System.nanoTime() - start;
      lastReading = elapsedTime - lastReading;

      if (mode != Mode.NOP) {

        // Read all the jiffies of the entire application
        if (readJiffies)
          for (File f: new File("/proc/" + GLIBC.getProcessId() + "/task/").listFiles()) {
            measure = new ArrayList<Object>();
            int id = Integer.parseInt(f.getName());

            measure.add(epoch);
            measure.add(id);
            for (String reading: GLIBC.getOSStats(id, (epoch % osReadingFactor) == 0))
              measure.add(reading);

            application.add(measure);
          }

        int tail = GLIBC.toAdd.size();
        for (int i = head; i < tail; i++)
          threadSet.add(GLIBC.toAdd.get(i));

        List<Thread> toRemove = new ArrayList<Thread>();
        for(Thread thread: threadSet) {
          if (thread != null && thread.isAlive()) {
            measure = new ArrayList<Object>();
            measure.add(epoch);
            measure.add(unixTime);

            measure.add(thread.getName());
            measure.add(thread.getId());

            measure.add(GLIBC.tids.get(thread));

            for (String reading: GLIBC.getOSStats(thread, (epoch % osReadingFactor) == 0))
              measure.add(reading);

            if (thread == Thread.currentThread())
              measure.add((double)chappieReadingTime / lastReading);
            else if (thread.getState() == Thread.State.RUNNABLE)
              measure.add(1.0);
            else
              measure.add(0.0);

            if (readMemory) measure.add(bean.getThreadAllocatedBytes(thread.getId()));

            threads.add(measure);
          } else if (thread != null) toRemove.add(thread);
        }
        for (Thread thread: toRemove)
          threadSet.remove(thread);

        jiffies.add(GLIBC.getJiffies(readJiffies && epoch % osReadingFactor == 0));

        if (lastRAPLReading.length == 0 || epoch % jraplReadingFactor == 0)
          lastRAPLReading = jrapl.EnergyCheckUtils.getEnergyStats();

        chappieReadingTime = (System.nanoTime() - start) - elapsedTime;

        for (int i = 0; i < lastRAPLReading.length / 3; ++i) {
          measure = new ArrayList<Object>();

          measure.add(epoch);
          measure.add(elapsedTime);
          measure.add(chappieReadingTime);

          measure.add(i + 1);
          measure.add(lastRAPLReading[3 * i + 2]);
          measure.add(lastRAPLReading[3 * i]);

          energy.add(measure);
        }

        epoch++;

        if(test_online_attribution) {
          if(epoch%epoch_len==0 && epoch>0) {
            int start_ep = epoch-epoch_len ;
            int end_ep = epoch;
            lastEpoch = end_ep;
            if(start_ep < end_ep) {
              //System.out.println("Start:"+start_ep+":end:"+end_ep);
              Map<Integer, List<ThreadEnergyAttribution>> tempMap; // = new HashMap<Integer, List<ThreadEnergyAttribution>>();
              tempMap = attrib.get_all_thread_attrib(start_ep, end_ep-1, epoch_len);
              //test_online_attribution = false;
              for(int i : tempMap.keySet()){
                List<ThreadEnergyAttribution> teaList = tempMap.get(i);
                for(ThreadEnergyAttribution tea : teaList){
                  StringBuilder energyString = new StringBuilder();
                  energyString.append(tea.getEpoch_no());
                  energyString.append(",");
                  energyString.append(tea.getCore_no());
                  energyString.append(",");
                  energyString.append(tea.getDram_energy());
                  energyString.append(",");
                  energyString.append(tea.getPkg_energy());
                  energyString.append(",");
                  energyString.append(tea.getTid());
                  energyList.add(new String(energyString));

                  dram_total += tea.getDram_energy();
                  package_total += tea.getPkg_energy();
                }
              }
            }
          }
        }

      }
    } else {
      timer.cancel();
      terminated = true;
    }
  }

  public void dismiss() {
    /*for(String s : energyList){
      System.out.println(s);
      }*/
    int start_ep = lastEpoch ;
    int end_ep = epoch;
    int new_epoch_len = (end_ep - start_ep) ;
    end_ep = new_epoch_len%2 == 0 ? new_epoch_len : new_epoch_len-1;
    if(test_online_attribution){
      if(start_ep < end_ep) {
        //System.out.println("Start:"+start_ep+":end:"+end_ep);

        Map<Integer, List<ThreadEnergyAttribution>> tempMap; // = new HashMap<Integer, List<ThreadEnergyAttribution>>();
        tempMap = attrib.get_all_thread_attrib(start_ep, end_ep, new_epoch_len);
        //test_online_attribution = false;
        for(int i : tempMap.keySet()){
          List<ThreadEnergyAttribution> teaList = tempMap.get(i);
          for(ThreadEnergyAttribution tea : teaList){
            StringBuilder energyString = new StringBuilder();
            energyString.append(tea.getEpoch_no());
            energyString.append(",");
            energyString.append(tea.getCore_no());
            energyString.append(",");
            energyString.append(tea.getDram_energy());
            energyString.append(",");
            energyString.append(tea.getPkg_energy());
            energyString.append(",");
            energyString.append(tea.getTid());
            energyList.add(new String(energyString));

            dram_total += tea.getDram_energy();
            package_total += tea.getPkg_energy();
          }
        }
      }
    }

    if (mode != Mode.NOP) {
      terminate = true;
      while(!terminated) {
        try {
          Thread.sleep(0, 100);
        } catch(Exception e) { }
      }
    }
    retire();
    System.out.println("FINAL TOTAL PACKAGE = "+package_total);
    System.out.println("FINAL TOTAL DRAM = "+dram_total);
  }

  private void retire() {

    /*for(String s : energyList){
      System.out.println(s);
      }*/
    PrintWriter log = null;
    String path = "chappie.runtime.csv";
    String message = "" + elapsedTime;
    System.out.println("ELAPSED TIME: "+ elapsedTime);
    try {
      log = new PrintWriter(new BufferedWriter(new FileWriter(path)));
    } catch (Exception io) {
      System.err.println("Error: " + io.getMessage());
    }

    log.write(message);
    log.close();

    if (mode != Mode.NOP) {
      path = "chappie.application.csv";
      try {
        log = new PrintWriter(new BufferedWriter(new FileWriter(path)));
      } catch (Exception io) {
        System.err.println("Error: " + io.getMessage());
      }

      message = "epoch,tid,core,u_jiffies,k_jiffies\n";
      log.write(message);

      for (List<Object> frame: application) {
        message = "";
        for (Object item: frame)
          message += item.toString() + ",";
        message = message.substring(0, message.length() - 1);
        message += "\n";
        log.write(message);
      }
      log.close();

      path = "chappie.thread.csv";
      try {
        log = new PrintWriter(new BufferedWriter(new FileWriter(path)));
      } catch (Exception io) {
        System.err.println("Error: " + io.getMessage());
      }

      message = "epoch,time,thread,pid,tid,core,u_jiffies,k_jiffies,state";

      if (readMemory)
        message += ",bytes";

      message += "\n";

      log.write(message);
      for (List<Object> frame : threads) {
        message = "";

        try {
          for (Object item: frame)
            message += item.toString() + ",";

          message = message.substring(0, message.length() - 1);
          message += "\n";
          log.write(message);
        } catch (Exception e) { }
      }
      log.close();

      path = "chappie.jiffies.csv";
      try {
        log = new PrintWriter(new BufferedWriter(new FileWriter(path)));
      } catch (Exception io) {
        System.err.println("Error: " + io.getMessage());
      }

      if (readJiffies)
        for (String jiffy : jiffies)
          log.write(jiffy + "\n");
      else
        log.write("\n");
      log.close();

      path = "chappie.trace.csv";
      try {
        log = new PrintWriter(new BufferedWriter(new FileWriter(path)));
      } catch (Exception io) {
        System.err.println("Error: " + io.getMessage());
      }

      message = "epoch,time,diff,socket,package,dram\n";
      log.write(message);

      for (List<Object> frame: energy) {
        message = "";
        for (Object item: frame)
          message += item.toString() + ",";
        message = message.substring(0, message.length() - 1);
        message += "\n";
        log.write(message);
      }
      log.close();

      //Online Attrib
      path = "chappie.online.csv";
      try{
        log = new PrintWriter(new BufferedWriter(new FileWriter(path)));
      } catch(Exception io) {
        System.err.println("Error: " + io.getMessage());
      }

      message = "epoch,core,dram,package,tid\n";
      log.write(message);
      for(String s : energyList){
        message = s;
        message += "\n";
        log.write(message);
      }
      log.close();

    }
  }

  public static void main(String[] args) throws IOException {
    GLIBC.getProcessId();
    GLIBC.getThreadId();

    int epoch_len = 10;
    try {
      epoch_len = Integer.parseInt(System.getenv("EPOCH_LENGTH"));
    } catch(Exception exc) {

    }


    boolean test_online_attribution=true;
    try {
      test_online_attribution = Boolean.parseBoolean(System.getenv("ONLINE_TEST"));
    } catch(Exception e) { }


    int online_frequency = 0;
    try {
      online_frequency = Integer.parseInt(System.getenv("ONLINE_TEST_FREQUENCY"));
    } catch(Exception exc) {

    }



    for (Thread thread: Thread.getAllStackTraces().keySet())
      if (!GLIBC.tids.containsKey(thread)) {
        GLIBC.tids.put(thread, -1);
        GLIBC.toAdd.add(thread);
      }

    int iterations = 10;
    try {
      iterations = Integer.parseInt(System.getenv("ITERS"));
    } catch(Exception e) { }

    Mode mode = Mode.SAMPLE;
    try {
      mode = Mode.valueOf(System.getenv("MODE"));
    } catch(Exception e) { }

    int polling = 1;
    try {
      polling = Integer.parseInt(System.getenv("POLLING"));
    } catch(Exception e) { }

    int osRate = 1;
    try {
      osRate = Integer.parseInt(System.getenv("CORE_RATE"));
    } catch(Exception e) { }

    int jraplRate = 1;
    try {
      jraplRate = Integer.parseInt(System.getenv("JRAPL_RATE"));
    } catch(Exception e) { }

    boolean readMemory = true;
    try {
      readMemory = Boolean.parseBoolean(System.getenv("MEMORY"));
    } catch(Exception e) { }

    boolean readJiffies = false;
    try {
      readJiffies = Boolean.parseBoolean(System.getenv("READ_JIFFIES"));
    } catch(Exception e) { }

    System.out.println("Number of Iterations : " + iterations);
    System.out.println("Chaperone Parameters:" +
        "\n - Mode:\t\t\t" + mode +
        "\n - Polling Rate:\t\t" + polling + " milliseconds" +
        "\n - OS Reading Factor:\t\t" + osRate +
        "\n - JRAPL Reading Factor:\t" + jraplRate +
        "\n - Memory Readings:\t\t" + readMemory +
        "\n - System Jiffies Readings:\t" + readJiffies);

    URLClassLoader loader;

    try {
      System.out.println("Loading " + args[0]);
      loader = new URLClassLoader(new URL[] {new URL(args[0])});
      Method main = loader.loadClass(args[1]).getMethod("main", String[].class);

      try {
        List<String> params = new ArrayList<String>();
        for (int i = 2; i < args.length; ++i) {
          String[] temp_params = args[i].split(" ", 100);
          for (int k = 0; k < temp_params.length; ++k)
            params.add(temp_params[k]);
        }

        for(int i = 0; i < iterations; ++i) {
          System.out.println("Iteration " + (i + 1));
          System.out.println("Running " + args[1] + ".main");
          System.out.println("Arguments: " + params.toString());
          System.out.println("==================================================");

          long start = System.nanoTime();
          Chaperone chaperone = new Chaperone(mode, polling, osRate, jraplRate, readMemory, readJiffies,test_online_attribution, epoch_len);
          main.invoke(null, (Object)params.toArray(new String[params.size()]));

          /*if(test_online_attribution) {
            OnlineTester onlineTester = new OnlineTester();
            onlineTester.setChappie(chaperone);
            onlineTester.setFrequency(online_frequency);
            Thread tester_thread = new Thread(onlineTester);
            tester_thread.setDaemon(true);
            System.out.println("Online Attribution Testing Is Enabled. Stay Tuned!");
            tester_thread.start();

            }*/

          System.out.println("==================================================");
          System.out.println(args[1] + " ran in " + String.format("%4f", (double)(System.nanoTime() - start) / 1000000000) + " seconds");
          System.out.println("Dismissing the chaperone");
          chaperone.dismiss();

          Files.move(Paths.get("chappie.runtime.csv"), Paths.get("chappie.runtime." + i + ".csv"));
          Files.move(Paths.get("chappie.application.csv"), Paths.get("chappie.application." + i + ".csv"));
          Files.move(Paths.get("chappie.thread.csv"), Paths.get("chappie.thread." + i + ".csv"));
          Files.move(Paths.get("chappie.jiffies.csv"), Paths.get("chappie.jiffies." + i + ".csv"));
          Files.move(Paths.get("chappie.trace.csv"), Paths.get("chappie.trace." + i + ".csv"));
        }
      } catch(Exception e) {
        System.out.println("Unable to bootstrap " + args[1] + ": " + e);
        e.printStackTrace();
      }
    } catch(Exception e) {
      System.out.println("Unable to load " + args[0] + ": " + e);
      e.printStackTrace();
    }




    Runtime.getRuntime().halt(0);
  }
}
