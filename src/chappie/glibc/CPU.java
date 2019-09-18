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
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sun.jna.Library;
import com.sun.jna.Native;

import chappie.util.ChappieLogger;

public class CPU {
  // System record require only reading of /proc/stat but it contains some
  // garbage (relative to us) so I made a little parser to help out later
  // when we want to write data
  String stat;
  private CPU(String stat) { this.stat = stat; }

  // this is a little cheat I made to help myself later
  private String[] stats;

  // cpu fields
  private int cpu;

  // jiffies
  private long user;
  private long nice;
  private long system;
  private long idle;
  private long iowait;
  private long irq;
  private long softirq;
  private long steal;
  private long guest;
  private long guestNice;

  public CPU parse() {
    if (stat != null) {
      stats = this.stat.split(" ");
      stats[0] = stats[0].substring(3, stats[0].length());

      cpu = Integer.parseInt(stats[0]);
      user = Long.parseLong(stats[1]);
      nice = Long.parseLong(stats[2]);
      system = Long.parseLong(stats[3]);
      idle = Long.parseLong(stats[4]);
      iowait = Long.parseLong(stats[5]);
      irq = Long.parseLong(stats[6]);
      softirq = Long.parseLong(stats[7]);
      steal = Long.parseLong(stats[8]);
      guest = Long.parseLong(stats[9]);
      guestNice = Long.parseLong(stats[10]);

      stat = null;
    }

    return this;
  }

  @Override
  public String toString() {
    return String.join(";", stats);
  }

  private static String path = "/proc/stat";
  private static int socketNum = Runtime.getRuntime().availableProcessors();

  public static CPU[] getCPUs() throws IOException {
    CPU[] stats = new CPU[socketNum];

    BufferedReader reader = new BufferedReader(new FileReader(path));

    // we are throwing away the first record; it's the whole system and
    // we need cpu specific
    reader.readLine();
    for (int i = 0; i < socketNum; i++)
      stats[i] = new CPU(reader.readLine());

    reader.close();

    return stats;
  }
}
