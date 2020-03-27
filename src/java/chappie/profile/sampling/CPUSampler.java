/* ************************************************************************************************
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * Copyright 2019 SUNY Binghamton
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

package chappie.profile.sampling;

import chappie.profile.Record;
import java.util.ArrayList;
import jlibc.proc.CPU;
import jrapl.EnergyCheckUtils;

public final class CPUSampler {
  private static CPU[] last = CPU.getCPUs();

  public static CPURecord sample() {
    CPU[] current = CPU.getCPUs();
    CPURecord record = new CPURecord(last, current);
    last = current;
    return record;
  }

  public static class CPURecord implements Record {
    private final long[] jiffies = new long[EnergyCheckUtils.socketNum];

    private CPURecord(CPU[] first, CPU[] second) {
      for (int i = 0; i < first.length; i++) {
        long jiffies = second[i].getUserJiffies() - first[i].getUserJiffies();
        jiffies += second[i].getNiceJiffies() - first[i].getNiceJiffies();
        jiffies += second[i].getKernelJiffies() - first[i].getKernelJiffies();
        jiffies += second[i].getIOWaitJiffies() - first[i].getIOWaitJiffies();
        jiffies += second[i].getIRQJiffies() - first[i].getIRQJiffies();
        jiffies += second[i].getSoftIRQJiffies() - first[i].getSoftIRQJiffies();
        jiffies += second[i].getStealJiffies() - first[i].getStealJiffies();
        jiffies += second[i].getGuestJiffies() - first[i].getGuestJiffies();
        jiffies += second[i].getGuestNiceJiffies() - first[i].getGuestNiceJiffies();
        this.jiffies[(int)(i / 20)] += jiffies;
      }
    }

    public long getJiffies(int socket) {
      return jiffies[socket];
    }

    @Override
    public String toString() {
      String message = "";
      for (long jiffy: jiffies) {
        message += jiffy + ",";
      }
      return message.substring(0, message.length() - 1);
    }
  }
}
