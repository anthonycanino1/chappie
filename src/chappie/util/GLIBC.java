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

package chappie.util;

import java.io.*;

import java.util.Set;
import java.util.HashSet;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import com.sun.jna.Library;
import com.sun.jna.Native;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

import java.nio.charset.StandardCharsets;

import java.nio.file.Files;
import java.nio.file.Paths;

interface GLIBCLibrary extends Library {
  static GLIBCLibrary glibc = (GLIBCLibrary)Native.loadLibrary("c", GLIBCLibrary.class);

  int syscall(int number, Object... args);
}

public abstract class GLIBC {
  static int getpid() { return GLIBCLibrary.glibc.syscall(39); }

  static Integer pid = getpid();
  public static int getProcessId() { return pid; }

  static int gettid() { return GLIBCLibrary.glibc.syscall(186); }

  public static Thread main = Thread.currentThread();
  public static List<Thread> toAdd = new ArrayList<Thread>();
  public static Map<Thread, Integer> tids = new ConcurrentHashMap<Thread, Integer>();

  public static int getThreadId() {
    Thread thread = Thread.currentThread();

    if (!tids.containsKey(thread) || thread != main) {
      toAdd.add(thread);
      tids.put(thread, gettid());
    }

    return tids.get(thread);
  }

  public static void unmapThread() {
    Thread thread = Thread.currentThread();

    tids.remove(thread);
  }

  private final static String[] DEFAULT_OS_READING = new String[] {"-1", "0", "0"};

  private static Map<Integer, String[]> lastOSReading = new HashMap<Integer, String[]>();

  public static String[] getOSStats(Thread thread, boolean read) {
    if (tids.containsKey(thread) && tids.get(thread) > -1)
      return getOSStats(tids.get(thread), read);
    else
      return DEFAULT_OS_READING;
  }

  public static String[] getOSStats(int tid, boolean read) {
    try {
      if (read || !lastOSReading.containsKey(tid)) {
        String path = "/proc/" + pid + "/task/" + tid + "/stat";
        BufferedReader reader = new BufferedReader(new FileReader(path));
        String message = reader.readLine();
        reader.close();

        String[] messages = message.split(" ");
        lastOSReading.put(tid, new String[] {messages[38], messages[13], messages[14]});
      }
    } catch(Exception e) { }

    if (lastOSReading.containsKey(tid))
      return lastOSReading.get(tid);
    else
      return DEFAULT_OS_READING;
  }

  private static String lastJiffies = "";

  public static String getJiffies(boolean read) {
    if (read)
    	try {
        lastJiffies = new String(Files.readAllBytes(Paths.get("/proc/stat")));
	    } catch(Exception e) { }

    return lastJiffies;
  }
}
