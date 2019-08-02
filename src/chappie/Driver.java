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
// import chappie.input.Config;
// import chappie.Chaperone;
//
// import java.lang.reflect.Method;
//
// import java.io.IOException;
//
// import java.net.URL;
// import java.net.URLClassLoader;
//
// import java.util.ArrayList;
//
// public class Main {
//   public static void main(String[] args) throws IOException {
//     String configPath = System.getProperty("chappie.config", null);
//     if (configPath == null) {
//       System.out.println("no config found");
//       System.exit(0);
//     }
//     Config config = Config.readConfig(configPath, "");
//     System.out.println(config.toString());
//
//     String jarPath = args[0];
//     String mainClass = args[1];
//
//     if (config != null) {
//       URLClassLoader loader;
//
//       try {
//         System.out.println("Loading " + jarPath);
//
//         loader = new URLClassLoader(new URL[] {new URL(jarPath)});
//         Method main = loader.loadClass(args[1]).getMethod("main", String[].class);
//
//         try {
//           String jarArgs = args[2];
//           String[] jarParams = jarArgs.split(" ");
//
//           System.out.println("Running " + mainClass + ".main");
//           System.out.println("Arguments: " + jarParams.toString());
//           System.out.println("==================================================");
//
//           Chaperone chaperone = new Chaperone(config);
//           main.invoke(null, (Object)jarParams);
//
//           System.out.println("==================================================");
//           System.out.println("Dismissing the chaperone");
//           chaperone.cancel();
//
//         } catch(Exception e) {
//           System.out.println("Unable to bootstrap " + mainClass + ": " + e);
//           e.printStackTrace();
//         }
//       } catch(Exception e) {
//         System.out.println("Unable to load " + jarPath + ": " + e);
//         e.printStackTrace();
//       }
//
//       // We are using an agent to catch System.exit(),
//       // so we have to terminate with this,
//       // otherwise some threads may continue running
//       // and not become orphans.
//       Runtime.getRuntime().halt(0);
//     }
//
//     // chappie.input.Parser parser = new chappie.input.Parser(configPath);
//
//   /*
//   // should be handled by highest level call (benchmark, grid search)
//   System.out.println("Starting Chappie ... Stay tuned!");
// 	int sockets_no = -1;
// 	try {
// 		sockets_no = Integer.parseInt(System.getenv("SOCKETS_NO"));
// 	} catch(Exception exc) {};
//
// 	boolean no_rapl=false;
//     try {
// 	no_rapl = Boolean.parseBoolean(System.getenv("NO_RAPL"));
//     } catch(Exception exc) { }
//
//
//     boolean gem5_cmdline_dumpstats=false;
//     try {
// 	gem5_cmdline_dumpstats = Boolean.parseBoolean(System.getenv("GEM5_CMDLINE_DUMPSTATS"));
//     } catch(Exception exc) { }
//
//
//     int early_exit = -1;
//     try {
//       early_exit = Integer.parseInt(System.getenv("EARLY_EXIT"));
//     } catch(Exception e) { }
//
//
// 	ChappieMode mode = ChappieMode.FULL;
//     try {
//       mode = ChappieMode.valueOf(System.getenv("MODE"));
//     } catch(Exception e) { }
//
//     int vmPolling = 4;
//     try {
//       vmPolling = Integer.parseInt(System.getenv("VM_POLLING"));
//     } catch(Exception e) { }
//
//     int osPolling = 1;
//     try {
//       osPolling = Integer.parseInt(System.getenv("OS_POLLING"));
//     } catch(Exception e) { }
//
//     int hpPolling = 4;
//     try {
//       hpPolling = Integer.parseInt(System.getenv("HP_POLLING"));
//     } catch(Exception e) { }
//
//     // System.out.println("Number of Iterations : " + iterations);
//     System.out.println("Chaperone Parameters:" +
//                         "\n - Mode:\t\t\t" + mode +
//                         "\n - VM Polling Rate:\t\t" + vmPolling + " milliseconds" +
//                         "\n - OS Polling Rate:\t\t" + vmPolling * osPolling + " milliseconds" +
//                         "\n - HP Polling Rate:\t\t" + hpPolling + " milliseconds" +
//             						"\n - No Rapl:\t\t" + no_rapl + " ." +
//             						"\n - Dump Gem5 Stats:\t\t" + gem5_cmdline_dumpstats + " ." +
//             						"\n - Early Exit:\t\t" + early_exit + " ." +
//             						"\n - Number of Sockets:\t\t" + sockets_no
//                       );
//
//     URLClassLoader loader;
//
//     try {
//       System.out.println("Loading " + jarPath);
//       loader = new URLClassLoader(new URL[] {new URL(args[0])});
//       Method main = loader.loadClass(args[1]).getMethod("main", String[].class);
//
//       try {
//         // need a change here to support -Dargs
//         ArrayList<String> params = new ArrayList<String>();
//         for (int i = 2; i < args.length; ++i) {
//           String[] temp_params = args[i].split(" ", 100);
//           for (int k = 0; k < temp_params.length; ++k)
//             params.add(temp_params[k]);
//         }
//
//         System.out.println("Running " + args[1] + ".main");
//         System.out.println("Arguments: " + params.toString());
//         System.out.println("==================================================");
//
//         long start = System.nanoTime();
//         Chaperone chaperone = new Chaperone(mode, vmPolling, osPolling,no_rapl,gem5_cmdline_dumpstats,early_exit,sockets_no);
//         main.invoke(null, (Object)params.toArray(new String[params.size()]));
//
//         System.out.println("==================================================");
//         System.out.println(args[1] + " ran in " + String.format("%4f", (double)(System.nanoTime() - start) / 1000000000) + " seconds");
//
//         System.out.println("Dismissing the chaperone");
//         chaperone.cancel();
//
//       } catch(Exception e) {
//         System.out.println("Unable to bootstrap " + args[1] + ": " + e);
//         e.printStackTrace();
//       }
//     } catch(Exception e) {
//       System.out.println("Unable to load " + args[0] + ": " + e);
//       e.printStackTrace();
//     }
//
//     // We are using an agent to catch System.exit(),
//     // so we have to terminate with this,
//     // otherwise some threads may continue running
//     // and not become orphans.
//     Runtime.getRuntime().halt(0);
//     */
//   }
// }
