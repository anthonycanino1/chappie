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

package chappie.profile.impl;

import java.io.IOException;
import java.util.ArrayList;

import asgct.ASGCTReader;

import chappie.Chaperone;
import chappie.profile.*;
import chappie.profile.util.*;

public class TraceProfiler extends Profiler {
  public TraceProfiler(int rate, int time, String workDirectory) {
    super(rate, time, workDirectory);
  }

  private static class TraceRecord extends Record {
    private ASGCTReader.ASGCTFrame frame;

    private TraceRecord(int epoch, ASGCTReader.ASGCTFrame frame) {
      this.epoch = epoch;
      this.frame = frame;
    }

    protected String stringImpl() {
      String message = frame.getId() + ";" + frame.getTimestamp() + ";";
      for (String method: frame.getTrace())
        message += method + "@";

      message = message.substring(0, message.length() - 1);

      return message;
    }

    private static final String[] header = new String[] { "epoch", "id", "timestamp", "trace" };
    public static String[] getHeader() {
      return header;
    };
  }

  protected void sampleImpl(int epoch) {
    for (ASGCTReader.ASGCTFrame record: ASGCTReader.fetch())
      data.add(new TraceRecord(epoch, record));
  }

  public void dumpImpl() throws IOException {
    CSV.write(data, TraceRecord.getHeader(), Chaperone.getWorkDirectory() + "/method.csv");
  }
}
