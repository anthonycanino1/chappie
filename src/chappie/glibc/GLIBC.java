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

package chappie.glibc;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sun.jna.Library;
import com.sun.jna.Native;

import chappie.util.ChappieLogger;

interface GLIBCLibrary extends Library {
  static GLIBCLibrary instance = (GLIBCLibrary)Native.loadLibrary("c", GLIBCLibrary.class);

  int getpid();
  int gettid();

  int syscall(int call);
}

public abstract class GLIBC {
  // pid methods
  private static int getpid() {
    try {
      return GLIBCLibrary.instance.getpid();
    } catch (UnsatisfiedLinkError e) {
      return -1;
    }
  }

  private static Integer pid = getpid();
  public static int getProcessId() { return pid; }

  // tid methods
  private static int gettid() {
    try {
      return GLIBCLibrary.instance.gettid();
    } catch (UnsatisfiedLinkError e) {
      return -1;
    }
  }

  private static HashMap<Object, Object> tidMap = new HashMap<Object, Object>();
  public static int getThreadId() {
    Logger logger = ChappieLogger.getLogger();

    int tid = -1;
    Thread thread = Thread.currentThread();
    if (!tidMap.containsKey(thread.getId())) {
      tid = GLIBC.gettid();
      if (tid > 0) {
        tidMap.put(thread.getId(), tid);
        logger.info("mapped " + thread.getName() + " to " + tid);
      } else logger.info("could not map " + thread.getName());
    } else tid = (int)tidMap.get(thread.getId());

    return tid;
  }

  // process reading helpers
  public static String readProcess(String tid) throws IOException {
    String path = "/proc/" + pid + "/task/" + tid + "/stat";
    BufferedReader reader = new BufferedReader(new FileReader(path));
    String stat = reader.readLine();
    reader.close();

    return stat;
  }

  private static HashMap<Object, Object> nameMap = new HashMap<Object, Object>();
  public static String[] parseProcessRecord(String stat) {
    String[] stats = stat.split(" ");
    int offset = stats.length - 52;

    String tid = stats[0];
    if (!nameMap.containsKey(tid)) {
      String name = Arrays.stream(Arrays.copyOfRange(stats, 1, 2 + offset)).collect(Collectors.joining(""));
      nameMap.put(tid, name.substring(1, name.length() - 1));
    }

    String state = stats[2 + offset];
    String u_jiffies = stats[13 + offset];
    String k_jiffies = stats[14 + offset];
    String core = stats[38 + offset];

    return new String[] {tid, core, state, u_jiffies, k_jiffies};
  }

  // runtime stats
  private static int cores = Runtime.getRuntime().availableProcessors();
  public static ArrayList<String> readSystem() throws IOException {
    ArrayList<String> records = new ArrayList<String>();
    BufferedReader reader = new BufferedReader(new FileReader("/proc/stat"));

    reader.readLine();
    for (int i = cores; i > 0; i--)
      records.add(reader.readLine());
    reader.close();

    return records;
  }

  public static String[] parseSystemRecord(String stat) {
    String[] stats = stat.split(" ");
    stats[0] = stats[0].substring(3, stats[0].length());

    return stats;
  }

  // helper functions
  public static void dumpMapping() throws IOException {
    chappie.util.JSON.write(tidMap, "data/tid.json");
    chappie.util.JSON.write(nameMap, "data/name.json");
  }
}
