// /* ************************************************************************************************
//  * Permission is hereby granted, free of charge, to any person obtaining a copy of this
//  * Copyright 2017 SUNY Binghamton
//  * software and associated documentation files (the "Software"), to deal in the Software
//  * without restriction, including without limitation the rights to use, copy, modify, merge,
//  * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
//  * persons to whom the Software is furnished to do so, subject to the following conditions:
//  *
//  * The above copyright notice and this permission notice shall be included in all copies or
//  * substantial portions of the Software.
//  *
//  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
//  * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
//  * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
//  * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
//  * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
//  * DEALINGS IN THE SOFTWARE.
//  * ***********************************************************************************************/
//
// package chappie;
//
// import chappie.util.*;
//
// import java.util.List;
// import java.util.Arrays;
// import java.util.ArrayList;
//
// import java.util.Set;
// import java.util.HashSet;
//
// import java.util.Map;
// import java.util.HashMap;
// import java.util.TreeMap;
// import java.util.concurrent.ConcurrentHashMap;
//
// import java.io.*;
//
// import java.nio.file.Paths;
// import java.nio.file.Files;
//
// import java.lang.management.*;
// import com.sun.management.*;
//
// import jrapl.EnergyCheckUtils.*;
//
// import java.lang.Thread;
// import java.lang.reflect.Method;
//
// import java.net.URL;
// import java.net.URLClassLoader;
//
// import java.util.Timer;
// import java.util.TimerTask;
//
// public class Chaperone9 extends TimerTask {
//   public enum Mode {NOP, SAMPLE}
//
//   // Chaperone Parameters
//   private Mode mode;
//   private int polling;
//
//   // Settings
//   private boolean readJiffies;
//   private int osReadingFactor;
//   private int jraplReadingFactor;
//
//   // Metrics
//   private int epoch = 0;
//   private com.sun.management.ThreadMXBean bean;
//   private long start;
//   private long elapsedTime = 0;
//   private long chappieReadingTime = 0;
//
//   private Timer timer;
//
//   public Chaperone9(Mode mode, int polling, int osRate, int jraplRate, boolean readJiffies) {
//     this.mode = mode;
//
//     this.polling = polling;
//
//     this.readJiffies = readJiffies;
//     this.osReadingFactor = osRate;
//     this.jraplReadingFactor = jraplRate;
//
//     this.epoch = 0;
//     bean = (com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
//
//     start = System.nanoTime();
//
//     if (mode != Mode.NOP) {
//       timer = new Timer("Chaperone");
//       timer.scheduleAtFixedRate(this, 0, polling);
//     }
//   }
//
//   // Runtime data containers
//   private List<Double> activeness = new ArrayList<Double>();
//
//   private List<List<Object>> threadData = new ArrayList<List<Object>>();
//   private List<List<Object>> energyData = new ArrayList<List<Object>>();
//   private List<String> jiffiesData = new ArrayList<String>();
//   // private List<List<Object>> threads = new ArrayList<List<Object>>();
//
//   // Management for lazy/staggered sampling
//   // private int head = 0;
//   // private Set<Thread> threadSet = new HashSet<Thread>();
//
//   private double[] lastRAPLReading = new double[0];
//
//   // Termination flags
//   // The timer class does not have a proper termination method. Luckily,
//   // only main touches the chaperone, so we can use a double flag psuedo-lock.
//   private boolean terminate = false;
//   private boolean terminated = false;
//
//   @Override
//   public void run() {
//     // Check if we need to stop
//     if (!terminate) {
//       List<Object> measure;
//       long unixTime = System.currentTimeMillis();
//
//       // set this epoch and cache last epoch's nano timestamp
//       long lastEpochTime = elapsedTime;
//       elapsedTime = System.nanoTime() - start;
//       lastEpochTime = elapsedTime - lastEpochTime;
//
//       if (mode != Mode.NOP) {
//         // Read jiffies of this entire application
//         if (readJiffies && epoch % osReadingFactor == 0) {
//           for (File f: new File("/proc/" + GLIBC.getProcessId() + "/task/").listFiles()) {
//             measure = new ArrayList<Object>();
//             int id = Integer.parseInt(f.getName());
//
//             measure.add(epoch);
//             measure.add(id);
//             measure.add(unixTime);
//             measure.add(GLIBC.getOSStats9(id, (epoch % osReadingFactor) == 0));
//
//             threadData.add(measure);
//           }
//         }
//
//         // Read jiffies of system
//         jiffiesData.add(GLIBC.getJiffies(readJiffies && epoch % osReadingFactor == 0));
//
//         if (lastRAPLReading.length == 0 || epoch % jraplReadingFactor == 0)
//           lastRAPLReading = jrapl.EnergyCheckUtils.getEnergyStats();
//
//         for (int i = 0; i < lastRAPLReading.length / 3; ++i) {
//           measure = new ArrayList<Object>();
//
//           measure.add(epoch);
//           measure.add(elapsedTime);
//           measure.add(chappieReadingTime);
//
//           measure.add(i + 1);
//           measure.add(lastRAPLReading[3 * i + 2]);
//           measure.add(lastRAPLReading[3 * i]);
//
//           energyData.add(measure);
//         }
//
//         epoch++;
//
//         chappieReadingTime = (System.nanoTime() - start) - elapsedTime;
//         activeness.add((double)chappieReadingTime / lastEpochTime);
//       }
//     } else {
//       timer.cancel();
//       terminated = true;
//     }
//   }
//
//   public void dismiss(String directory, String iter) {
//     if (mode != Mode.NOP) {
//       terminate = true;
//       while(!terminated) {
//         try {
//           Thread.sleep(0, 100);
//         } catch(Exception e) { }
//       }
//     }
//     retire(directory, iter);
//   }
//
//   private void retire(String directory, String iter) {
//     PrintWriter log = null;
//     String path = Paths.get(directory, "chappie.runtime" + iter + ".csv").toString();
//     try {
//       log = new PrintWriter(new BufferedWriter(new FileWriter(path)));
//     } catch (Exception io) {
//       System.err.println("Error: " + io.getMessage());
//     }
//
//     String message = "name,value\nruntime," + elapsedTime + "\nmain_id," + GLIBC.getThreadId();
//     log.write(message);
//     log.close();
//
//     if (mode != Mode.NOP) {
//       // write the frame data
//       path = Paths.get(directory, "chappie.thread" + iter + ".csv").toString();
//       try {
//         log = new PrintWriter(new BufferedWriter(new FileWriter(path)));
//       } catch (Exception io) {
//         System.err.println("Error: " + io.getMessage());
//       }
//
//       message = "epoch,tid,timestamp,record\n";
//       log.write(message);
//
//       for (List<Object> frame: threadData) {
//         message = "";
//         for (Object item: frame)
//           message += item.toString() + ",";
//         message = message.substring(0, message.length() - 1);
//         message += "\n";
//         log.write(message);
//       }
//       log.close();
//
//       path = Paths.get(directory, "chappie.trace" + iter + ".csv").toString();
//       try {
//         log = new PrintWriter(new BufferedWriter(new FileWriter(path)));
//       } catch (Exception io) {
//         System.err.println("Error: " + io.getMessage());
//       }
//
//       message = "epoch,time,diff,socket,package,dram\n";
//       log.write(message);
//
//       for (List<Object> frame: energyData) {
//         message = "";
//         for (Object item: frame)
//           message += item.toString() + ",";
//         message = message.substring(0, message.length() - 1);
//         message += "\n";
//         log.write(message);
//       }
//       log.close();
//
//       // write the list data
//       path = Paths.get(directory, "chappie.jiffies" + iter + ".csv").toString();
//       try {
//         log = new PrintWriter(new BufferedWriter(new FileWriter(path)));
//       } catch (Exception io) {
//         System.err.println("Error: " + io.getMessage());
//       }
//
//       log.write("record\n");
//
//       if (readJiffies) {
//           epoch = 0;
//   	      for (String record : jiffiesData) {
//   	        log.write(record + "\n");
//           }
//       } else
//   		      log.write("\n");
//       log.close();
//
//       path = Paths.get(directory, "chappie.activeness" + iter + ".csv").toString();
//       try {
//         log = new PrintWriter(new BufferedWriter(new FileWriter(path)));
//       } catch (Exception io) {
//         System.err.println("Error: " + io.getMessage());
//       }
//
//       message = "epoch,activeness\n";
//
//       log.write(message);
//       int epoch = 0;
//       for (Double activity : activeness)
//         log.write(epoch++ + "," + activity + "\n");
//       log.close();
//     }
//   }
// }
