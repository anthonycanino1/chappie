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
// public class Chaperone extends TimerTask {
//   public enum Mode {NOP, SAMPLE}
//
//   // Chaperone Parameters
//   private Mode mode;
//
//   private int polling;
//   private int osReadingFactor;
//   private int jraplReadingFactor;
//
//   private boolean readMemory;
//   private boolean readJiffies;
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
//   public Chaperone(Mode mode, int polling, int osRate, int jraplRate, boolean memory, boolean readJiffies) {
//     this.mode = mode;
//
//     this.polling = polling;
//     this.osReadingFactor = osRate;
//     this.jraplReadingFactor = jraplRate;
//
//     this.readMemory = memory;
//     this.readJiffies = readJiffies;
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
//   private List<List<Object>> application = new ArrayList<List<Object>>();
//   private List<List<Object>> threads = new ArrayList<List<Object>>();
//   private List<List<Object>> energy = new ArrayList<List<Object>>();
//   private ArrayList<String> jiffies = new ArrayList<String>();
//
//   // Management for lazy/staggered sampling
//   private int head = 0;
//   private Set<Thread> threadSet = new HashSet<Thread>();
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
//       long lastReading = elapsedTime;
//       elapsedTime = System.nanoTime() - start;
//       lastReading = elapsedTime - lastReading;
//
//       if (mode != Mode.NOP) {
//         // Read all the jiffies of the entire application
//         if (readJiffies)
//           for (File f: new File("/proc/" + GLIBC.getProcessId() + "/task/").listFiles()) {
//             measure = new ArrayList<Object>();
//             int id = Integer.parseInt(f.getName());
//
//             measure.add(epoch);
//             measure.add(id);
//             for (String reading: GLIBC.getOSStats(id, (epoch % osReadingFactor) == 0))
//               measure.add(reading);
//
//             application.add(measure);
//           }
//
//         int tail = GLIBC.toAdd.size();
//         for (int i = head; i < tail; i++)
//           threadSet.add(GLIBC.toAdd.get(i));
//
//         head = tail;
//
//         List<Thread> toRemove = new ArrayList<Thread>();
//         for(Thread thread: threadSet) {
//           if (thread != null && thread.isAlive()) {
//             measure = new ArrayList<Object>();
//             measure.add(epoch);
//             measure.add(unixTime);
//
//             measure.add(thread.getName());
//             measure.add(thread.getId());
//
//             measure.add(GLIBC.tids.get(thread));
//
//             for (String reading: GLIBC.getOSStats(thread, (epoch % osReadingFactor) == 0))
//               measure.add(reading);
//
//             if (thread == Thread.currentThread())
//               measure.add((double)chappieReadingTime / lastReading);
//             else if (thread.getState() == Thread.State.RUNNABLE)
//               measure.add(1);
//             else
//               measure.add(0);
//
//         		if (readMemory) measure.add(bean.getThreadAllocatedBytes(thread.getId()));
//
//             threads.add(measure);
//           } else if (thread != null) toRemove.add(thread);
//         }
//         for (Thread thread: toRemove)
//           threadSet.remove(thread);
//
//         jiffies.add(GLIBC.getJiffies(readJiffies && epoch % osReadingFactor == 0));
//
//         if (lastRAPLReading.length == 0 || epoch % jraplReadingFactor == 0)
//           lastRAPLReading = jrapl.EnergyCheckUtils.getEnergyStats();
//
//         chappieReadingTime = (System.nanoTime() - start) - elapsedTime;
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
//           energy.add(measure);
//         }
//
//         epoch++;
//       }
//     } else {
//       timer.cancel();
//       terminated = true;
//     }
//   }
//
//   public void dismiss() {
//     if (mode != Mode.NOP) {
//       terminate = true;
//       while(!terminated) {
//         try {
//           Thread.sleep(0, 100);
//         } catch(Exception e) { }
//       }
//     }
//     retire();
//   }
//
//   private void retire() {
//     PrintWriter log = null;
//     String path = "chappie.runtime.csv";
//     String message = "" + elapsedTime;
//     try {
//       log = new PrintWriter(new BufferedWriter(new FileWriter(path)));
//     } catch (Exception io) {
//       System.err.println("Error: " + io.getMessage());
//     }
//
//     log.write(message);
//     log.close();
//
//     if (mode != Mode.NOP) {
//       path = "chappie.application.csv";
//       try {
//         log = new PrintWriter(new BufferedWriter(new FileWriter(path)));
//       } catch (Exception io) {
//         System.err.println("Error: " + io.getMessage());
//       }
//
//       message = "epoch,tid,core,u_jiffies,k_jiffies,name,state\n";
//       log.write(message);
//
//       for (List<Object> frame: application) {
//         message = "";
//         for (Object item: frame)
//           message += item.toString() + ",";
//         message = message.substring(0, message.length() - 1);
//         message += "\n";
//         log.write(message);
//       }
//       log.close();
//
//       path = "chappie.thread.csv";
//       try {
//         log = new PrintWriter(new BufferedWriter(new FileWriter(path)));
//       } catch (Exception io) {
//         System.err.println("Error: " + io.getMessage());
//       }
//
//       message = "epoch,time,thread,pid,tid,core,u_jiffies,k_jiffies,name,os_state,state";
//
//       if (readMemory)
//       message += ",bytes";
//
//       message += "\n";
//
//       log.write(message);
//       for (List<Object> frame : threads) {
//         message = "";
//
//         try {
//           for (Object item: frame)
//           message += item.toString() + ",";
//
//           message = message.substring(0, message.length() - 1);
//           message += "\n";
//           log.write(message);
//         } catch (Exception e) { }
//       }
//       log.close();
//
//       path = "chappie.jiffies.csv";
//       try {
//         log = new PrintWriter(new BufferedWriter(new FileWriter(path)));
//       } catch (Exception io) {
//         System.err.println("Error: " + io.getMessage());
//       }
//
//       if (readJiffies)
// 	      for (String jiffy : jiffies)
// 	        log.write(jiffy + "\n");
//       else
// 		      log.write("\n");
//       log.close();
//
//       path = "chappie.trace.csv";
//       try {
//         log = new PrintWriter(new BufferedWriter(new FileWriter(path)));
//       } catch (Exception io) {
//         System.err.println("Error: " + io.getMessage());
//       }
//
//       message = "epoch,time,diff,socket,package,dram\n";
//       log.write(message);
//
//       for (List<Object> frame: energy) {
//         message = "";
//         for (Object item: frame)
//           message += item.toString() + ",";
//         message = message.substring(0, message.length() - 1);
//         message += "\n";
//         log.write(message);
//       }
//       log.close();
//     }
//   }
//
//   public static void main(String[] args) throws IOException {
//     // try {
//     //   String path = "tids.txt";
//     //   PrintWriter log = new PrintWriter(new BufferedWriter(new FileWriter(path)));
//     //   int pid = GLIBC.getProcessId();
//     //   for (File f: new File("/proc/" + pid + "/task/").listFiles()) {
//     //     path = "/proc/" + pid + "/task/" + f.getName() + "/stat";
//     //     BufferedReader reader = new BufferedReader(new FileReader(path));
//     //     String message = reader.readLine();
//     //     reader.close();
//     //
//     //     log.write(message);
//     //     log.write("\n");
//     //   }
//     //   log.close();
//     // } catch(Exception e) { }
//
//     GLIBC.getProcessId();
//     GLIBC.getThreadId();
//
//     for (Thread thread: Thread.getAllStackTraces().keySet())
//       if (!GLIBC.tids.containsKey(thread)) {
//         GLIBC.tids.put(thread, -1);
//         GLIBC.toAdd.add(thread);
//       }
//
//     int iterations = 10;
//     try {
//       iterations = Integer.parseInt(System.getenv("ITERS"));
//     } catch(Exception e) { }
//
//     Mode mode = Mode.SAMPLE;
//     try {
//       mode = Mode.valueOf(System.getenv("MODE"));
//     } catch(Exception e) { }
//
//     int polling = 1;
//     try {
//       polling = Integer.parseInt(System.getenv("POLLING"));
//     } catch(Exception e) { }
//
//     int osRate = 1;
//     try {
//       osRate = Integer.parseInt(System.getenv("CORE_RATE"));
//     } catch(Exception e) { }
//
//     int jraplRate = 1;
//     try {
//       jraplRate = Integer.parseInt(System.getenv("JRAPL_RATE"));
//     } catch(Exception e) { }
//
//     boolean readMemory = true;
//     try {
//       readMemory = Boolean.parseBoolean(System.getenv("MEMORY"));
//     } catch(Exception e) { }
//
//     boolean readJiffies = false;
//     try {
//       readJiffies = Boolean.parseBoolean(System.getenv("READ_JIFFIES"));
//     } catch(Exception e) { }
//
//     System.out.println("Number of Iterations : " + iterations);
//     System.out.println("Chaperone Parameters:" +
//                         "\n - Mode:\t\t\t" + mode +
//                         "\n - Polling Rate:\t\t" + polling + " milliseconds" +
//                         "\n - OS Reading Factor:\t\t" + osRate +
//                         "\n - JRAPL Reading Factor:\t" + jraplRate +
//                         "\n - Memory Readings:\t\t" + readMemory +
//                         "\n - System Jiffies Readings:\t" + readJiffies);
//
//     URLClassLoader loader;
//
//     try {
//       System.out.println("Loading " + args[0]);
//       loader = new URLClassLoader(new URL[] {new URL(args[0])});
//       Method main = loader.loadClass(args[1]).getMethod("main", String[].class);
//
//       try {
//         List<String> params = new ArrayList<String>();
//         for (int i = 2; i < args.length; ++i) {
//           String[] temp_params = args[i].split(" ", 100);
//           for (int k = 0; k < temp_params.length; ++k)
//             params.add(temp_params[k]);
//         }
//
//         for(int i = 0; i < iterations; ++i) {
//           System.out.println("Iteration " + (i + 1));
//           System.out.println("Running " + args[1] + ".main");
//           System.out.println("Arguments: " + params.toString());
//           System.out.println("==================================================");
//
//           long start = System.nanoTime();
//           Chaperone chaperone = new Chaperone(mode, polling, osRate, jraplRate, readMemory, readJiffies);
//           main.invoke(null, (Object)params.toArray(new String[params.size()]));
//
//       	  System.out.println("==================================================");
//           System.out.println(args[1] + " ran in " + String.format("%4f", (double)(System.nanoTime() - start) / 1000000000) + " seconds");
//           System.out.println("Dismissing the chaperone");
//           chaperone.dismiss();
//
//           Files.move(Paths.get("chappie.runtime.csv"), Paths.get("chappie.runtime." + i + ".csv"));
//           Files.move(Paths.get("chappie.application.csv"), Paths.get("chappie.application." + i + ".csv"));
//           Files.move(Paths.get("chappie.thread.csv"), Paths.get("chappie.thread." + i + ".csv"));
//           Files.move(Paths.get("chappie.jiffies.csv"), Paths.get("chappie.jiffies." + i + ".csv"));
//           Files.move(Paths.get("chappie.trace.csv"), Paths.get("chappie.trace." + i + ".csv"));
//         }
//       } catch(Exception e) {
//         System.out.println("Unable to bootstrap " + args[1] + ": " + e);
//         e.printStackTrace();
//       }
//     } catch(Exception e) {
//       System.out.println("Unable to load " + args[0] + ": " + e);
//       e.printStackTrace();
//     }
//
//     Runtime.getRuntime().halt(0);
//   }
// }