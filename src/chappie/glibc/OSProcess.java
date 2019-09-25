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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;

import com.sun.jna.Library;
import com.sun.jna.Native;

import chappie.Chaperone;

public class OSProcess {

  private int id;
  private String statFile;
  private String taskDir;

  private OSProcess(int id) throws IOException {
    this.id = id;

    this.statFile = "/proc/" + GLIBC.getProcessId() + "/task/" + id + "/stat";
    this.taskDir = "/proc/" + id + "/task/";

    sample();
  }

  // private long timestamp;
  private String stat;

  public OSProcess sample() throws IOException {
    BufferedReader reader = new BufferedReader(new FileReader(this.statFile));
    this.stat = reader.readLine();
    reader.close();
    // timestamp = System.currentTimeMillis();

    return this;
  }

  public enum State {RUNNABLE, INTERRUPTIBLE, UNINTERRUPTIBLE, ZOMBIE, TERMINATED}
  private static OSProcess.State parseState(String state) {
    if (state.equals("R"))
      return OSProcess.State.RUNNABLE;
    else if (state.equals("S"))
      return OSProcess.State.INTERRUPTIBLE;
    else if (state.equals("D"))
      return OSProcess.State.UNINTERRUPTIBLE;
    else if (state.equals("Z"))
      return OSProcess.State.ZOMBIE;
    else if (state.equals("T"))
      return OSProcess.State.TERMINATED;
    else
      return null;
  }

  // stat fields we currently use; this could be extended further
  private int cpu;
  private OSProcess.State state;
  private long userJiffies;
  private long systemJiffies;

  private static HashMap<Integer, String> nameMap = new HashMap<Integer, String>();
  public OSProcess parse() {
    if (this.stat != null) {
      // since java use spaces in thread names, we can't just split on
      // space (the linux delimiter). We count the number of items, it should
      // be 52; any excess is the offset
      stats = stat.split(" ");
      int offset = stats.length - 52;

      if (!nameMap.containsKey(id)) {
        // I'm clipping the parens around thread name here so the output
        // is cleaner
        String name = String.join(" ", Arrays.copyOfRange(stats, 1, 2 + offset));
        name = name.substring(1, name.length() - 1);
        nameMap.put(id, name);
      }

      this.cpu = Integer.parseInt(stats[38 + offset]);
      this.state = OSProcess.parseState(stats[2 + offset]);
      this.userJiffies = Long.parseLong(stats[13 + offset]);
      this.systemJiffies = Long.parseLong(stats[14 + offset]);

      this.stat = null;
    }

    return this;
  }

  String[] stats;

  @Override
  public String toString() {
    return Integer.toString(id) + ";" +
      Integer.toString(cpu) + ";" +
      state.name() + ";" +
      Long.toString(userJiffies) + ";" +
      Long.toString(systemJiffies);
  }

  public ArrayList<OSProcess> getTasks() {
    File[] tasks = new File(this.taskDir).listFiles();
    ArrayList<OSProcess> procs = new ArrayList<OSProcess>(tasks.length);
    for (int i = 0; i < tasks.length; i++) {
      int tid = Integer.parseInt(tasks[i].getName());
      try {
        procs.add(new OSProcess(tid));
      } catch (IOException e) { }
    }

    return procs;
  }

  // I'm forcing us to stick to the current process because external
  // ones shouldn't matter other than stat and maybe other tools
  private static OSProcess current;
  public static OSProcess currentProcess() {
    if (current == null) {
      try {
        current = new OSProcess(GLIBC.getProcessId());
      } catch(IOException e) { }
    }

    return current;
  }

  public static void dump() throws IOException {
    chappie.util.JSON.write(nameMap, Chaperone.getWorkDirectory() + "/name.json");
  }
}
