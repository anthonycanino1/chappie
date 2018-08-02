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

import java.util.Map;
import java.util.HashMap;

import com.sun.jna.Library;
import com.sun.jna.Native;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

interface GLIBCLibrary extends Library {
  static GLIBCLibrary glibc = (GLIBCLibrary)Native.loadLibrary("c", GLIBCLibrary.class);

  int syscall(int number, Object... args);
}

public abstract class GLIBC {
  static int getpid() { return GLIBCLibrary.glibc.syscall(39); }

  static Integer pid = getpid();
  public static int getProcessId() { return pid; }

  static int gettid() { return GLIBCLibrary.glibc.syscall(186); }

  static Map<String, Integer> tids = new HashMap<String, Integer>();
  public static int getThreadId() {
    String name = Thread.currentThread().getName();
    if (!tids.containsKey(name) || name == "Chaperone")
      tids.put(name, gettid());

    return tids.get(name);
  }

  public static int[] getOSStats(String name) {
    String path = "/proc/" + pid + "/task/" + tids.get(name) + "/stat";

    try {
      BufferedReader reader = new BufferedReader(new FileReader(path));
      String message = reader.readLine();
      reader.close();

      String[] messages = message.split(" ");
      int[] values = new int[3];
      values[0] = Integer.parseInt(messages[38]);
      values[1] = Integer.parseInt(messages[14]);
      values[2] = Integer.parseInt(messages[15]);

      return values;
    } catch(Exception e) {
      return new int[] {-1, 0, 0};
    }
  }

  public static int[] getJiffies() {
    String path = "/proc/" + pid + "/stat";

    try {
      BufferedReader reader = new BufferedReader(new FileReader(path));
      String message = reader.readLine();
      reader.close();

      String[] messages = message.split(" ");
      int[] values = new int[2];
      values[0] = Integer.parseInt(messages[14]);
      values[1] = Integer.parseInt(messages[15]);

      return values;
    } catch(Exception e) {
      return new int[] {-1, 0, 0};
    }
  }

  public static int getCore(String name) {
    String path = "/proc/" + pid + "/task/" + tids.get(name) + "/stat";

    try {
      BufferedReader reader = new BufferedReader(new FileReader(path));
      String message = reader.readLine();
      reader.close();
      return Integer.parseInt(message.split(" ")[38]);
    } catch(Exception e) {
      return -1;
    }
  }

  public static Map<String, List<StackTraceElement>> callsites = new HashMap<String, List<StackTraceElement>>();

  public static void getCallSite(String name) {
    callsites.put(name, Arrays.asList(Thread.currentThread().getStackTrace()));

  }
}
