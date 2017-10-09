/* ************************************************************************************************
 * Copyright 2016 SUNY Binghamton
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

package chappie;

import java.util.Set;

import java.util.Map;
import java.util.TreeMap;

import java.io.*;

public abstract class Chaperone {
  protected Map<Long, Set<String>> timeLine = new TreeMap<Long, Set<String>>();

  public abstract void assign();
  public abstract void dismiss();

  public void retire() {

    String logname = System.getenv("CHAPPIE_LOG");
    if (logname == null) {
      logname = "chappie.log";
    }

    PrintWriter log = null;
    try {
      log = new PrintWriter(new BufferedWriter(new FileWriter(logname)));
    } catch (IOException io) {
      System.err.println("Error: " + io.getMessage());
      throw new RuntimeException("uh oh");
    }

    for(Long time : timeLine.keySet()) {
      String message = time + ": (";
      for(String name : timeLine.get(time)){
        message += name + ", ";
      }
      message = message.substring(0, message.length() - 2) + ")";
      log.write(message + "\n");
    }

    log.close();
  }
}
