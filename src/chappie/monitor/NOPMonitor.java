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

package chappie.monitor;

import chappie.util.GLIBC;

import java.util.List;

import java.io.*;

import java.nio.file.Path;
import java.nio.file.Paths;

public class NOPMonitor {
  // file helpers
  private String directory;
  private String suffix;

  public NOPMonitor() {
    // definition handled by parent caller (./chappie_test.sh)
    // directory management HAS to be handled by bootstrapper (./run.sh)
    // because of honest profiler (log path)
    directory = System.getenv("CHAPPIE_DIRECTORY");
    directory = directory != null ? directory : "";

    // helper due to the cold run problem we experienced around dacapo
    suffix = System.getenv("CHAPPIE_SUFFIX");
    suffix = suffix != null ? "." + suffix : "";
  }

  public void dump(long start, List<Double> activeness) {
    // // runtime stats
    // PrintWriter log = null;
    //
    // String path = Paths.get(directory, "chappie.runtime" + suffix + ".csv").toString();
    // try {
    //   log = new PrintWriter(new BufferedWriter(new FileWriter(path)));
    // } catch (Exception io) { }
    //
    // long runtime = System.currentTimeMillis() - start;
    // String message = "name,value\nruntime," + runtime + "\nmain_id," + GLIBC.getThreadId();
    // log.write(message);
    // log.close();
  }
}
