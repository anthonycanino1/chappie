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

public interface GLIBC extends Library {
  static GLIBC glibc = (GLIBC)Native.loadLibrary("c", GLIBC.class);

  int syscall(int number, Object... args);

  static int getpid() { return GLIBC.glibc.syscall(39); }

  static Integer pid = getpid();
  public static int getProcessId() { return pid; }

  static int gettid() { return GLIBC.glibc.syscall(186); }

  static Map<String, Integer> tids = new HashMap<String, Integer>();
  public static int getThreadId() {
    String name = Thread.currentThread().getName();
    if (!tids.containsKey(name))
      tids.put(name, gettid());

    return tids.get(name);
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
}
