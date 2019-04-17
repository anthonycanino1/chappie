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

import chappie.Chaperone.ChappieMode;
import chappie.util.GLIBC;

import java.lang.reflect.Method;

import java.io.IOException;

import java.net.URL;
import java.net.URLClassLoader;

import java.util.ArrayList;

public class Main {
  public static void main(String[] args) throws IOException {
    // should be handled by highest level call (benchmark, grid search)
    
	boolean no_rapl=false;
    try {
	no_rapl = Boolean.parseBoolean(System.getenv("NO_RAPL"));
    } catch(Exception exc) { }


    boolean gem5_cmdline_dumpstats=false;
    try {
	gem5_cmdline_dumpstats = Boolean.parseBoolean(System.getenv("GEM5_CMDLINE_DUMPSTATS"));
    } catch(Exception exc) { }

    
    int early_exit = -1;
    try {
      early_exit = Integer.parseInt(System.getenv("EARLY_EXIT"));
    } catch(Exception e) { }
	
	ChappieMode mode = ChappieMode.FULL;
    try {
      mode = ChappieMode.valueOf(System.getenv("MODE"));
    } catch(Exception e) { }

    int vmPolling = 4;
    try {
      vmPolling = Integer.parseInt(System.getenv("VM_POLLING"));
    } catch(Exception e) { }

    int osPolling = 4;
    try {
      osPolling = Integer.parseInt(System.getenv("OS_POLLING"));
    } catch(Exception e) { }

    int hpPolling = 4;
    try {
      hpPolling = Integer.parseInt(System.getenv("HP_POLLING"));
    } catch(Exception e) { }

    // System.out.println("Number of Iterations : " + iterations);
    System.out.println("Chaperone Parameters:" +
                        "\n - Mode:\t\t\t" + mode +
                        "\n - VM Polling Rate:\t\t" + vmPolling + " milliseconds" +
                        "\n - OS Polling Rate:\t\t" + osPolling + " milliseconds" +
                        "\n - HP Polling Rate:\t\t" + hpPolling + " milliseconds"
                      );

    URLClassLoader loader;

    try {
      System.out.println("Loading " + args[0]);
      loader = new URLClassLoader(new URL[] {new URL(args[0])});
      Method main = loader.loadClass(args[1]).getMethod("main", String[].class);

      try {
        // need a change here to support -Dargs
        ArrayList<String> params = new ArrayList<String>();
        for (int i = 2; i < args.length; ++i) {
          String[] temp_params = args[i].split(" ", 100);
          for (int k = 0; k < temp_params.length; ++k)
            params.add(temp_params[k]);
        }

        System.out.println("Running " + args[1] + ".main");
        System.out.println("Arguments: " + params.toString());
        System.out.println("==================================================");

        long start = System.nanoTime();
        Chaperone chaperone = new Chaperone(mode, vmPolling, osPolling);
        main.invoke(null, (Object)params.toArray(new String[params.size()]));

        System.out.println("==================================================");
        System.out.println(args[1] + " ran in " + String.format("%4f", (double)(System.nanoTime() - start) / 1000000000) + " seconds");

        System.out.println("Dismissing the chaperone");
        chaperone.cancel();

      } catch(Exception e) {
        System.out.println("Unable to bootstrap " + args[1] + ": " + e);
        e.printStackTrace();
      }
    } catch(Exception e) {
      System.out.println("Unable to load " + args[0] + ": " + e);
      e.printStackTrace();
    }

    // We are using an agent to catch System.exit(),
    // so we have to terminate with this,
    // otherwise some threads may continue running
    // and not become orphans.
    Runtime.getRuntime().halt(0);
  }
}
