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

import java.io.*;
import java.io.BufferedReader;
import java.io.FileReader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.HashMap;

import com.sun.jna.Library;
import com.sun.jna.Native;

interface GLIBCLibrary extends Library {
  static GLIBCLibrary instance = (GLIBCLibrary)Native.loadLibrary("c", GLIBCLibrary.class);

  int getpid();
  int gettid();
}

public abstract class GLIBC {
  // pid methods
  private static int getpid() { return GLIBCLibrary.instance.getpid(); }
  private static Integer pid = getpid();
  public static int getProcessId() { return pid; }

  // tid methods
  private static int gettid() { return GLIBCLibrary.instance.gettid(); }
  private static HashMap<Thread, Integer> tidMap = new HashMap<Thread, Integer>();
  public static int getThreadId() {
    Thread thread = Thread.currentThread();
    int tid = gettid();
    tidMap.put(thread, tid);

    return tid;
  }

  public static int getThreadId(Thread thread) {
    if (tidMap.containsKey(thread))
      return tidMap.get(thread);
    else
      return -1;
  }

  public static void dumpTidMap(Config config) {
    if (mode == Mode.SAMPLE) {
      CSVPrinter printer = new CSVPrinter(
        new FileWriter(config.workDirectory + "/chappie.tid" + config.suffix + ".csv"),
        CSVFormat.DEFAULT.withHeader("thread", "tid").withDelimiter(";")
      );

      printer.printRecords(data.entrySet());
      printer.close();

      GLIBC.tidMap.clear();
    }
  }

  // task stats
  public static String getThreadStats(int tid) {
    try {
      String path = "/proc/" + pid + "/task/" + tid + "/stat";
      BufferedReader reader = new BufferedReader(new FileReader(path));
      String message = reader.readLine();
      reader.close();

      return message;
    } catch(Exception e) {
      return "";
    }
  }

  public static String getThreadStats(Thread thread) {
    int tid = getThreadId(thread);
    if (tid > -1)
      return getThreadStats(tid);
    else
      return "";
  }

  // runtime stats
  private static int cores = Runtime.getRuntime().availableProcessors();
  public static ArrayList<String> getSystemStats() {
    try {
      ArrayList<String> records = new ArrayList<String>();
      BufferedReader reader = new BufferedReader(new FileReader("/proc/stat"));

      reader.readLine();
      for (int i = cores; i > 0; i--)
        records.add(reader.readLine());

      return records;
    } catch (Exception e) {
      return new ArrayList<String>();
    }
  }
}
