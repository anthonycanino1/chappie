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

import java.io.IOException;
import java.util.HashMap;

import com.sun.jna.Library;
import com.sun.jna.Native;

import chappie.Chaperone;

interface GLIBCLibrary extends Library {
  static GLIBCLibrary instance = (GLIBCLibrary)Native.loadLibrary("c", GLIBCLibrary.class);

  int getpid();
  int gettid();
}

public abstract class GLIBC {
  // pid methods
  private static int getpid() throws UnsatisfiedLinkError {
    return GLIBCLibrary.instance.getpid();
  }

  private static Integer pid = getpid();
  public static int getProcessId() { return pid; }

  // tid methods
  private static int gettid() throws UnsatisfiedLinkError {
    return GLIBCLibrary.instance.gettid();
  }

  // we are keeping a local copy of the tid map (essentially any thread that is created at runtime)
  // to speed things up a bit
  private static HashMap<Long, Integer> tidMap = new HashMap<Long, Integer>();
  public static int getTaskId() {
    int tid = -1;
    Thread thread = Thread.currentThread();
    if (!tidMap.containsKey(thread.getId())) {
      try {
        tid = GLIBC.gettid();
        if (tid > 0)
          tidMap.put(thread.getId(), tid);
      } catch (UnsatisfiedLinkError e) { }
    } else
      tid = (int)tidMap.get(thread.getId());

    return tid;
  }

  public static void dump() throws IOException {
    chappie.util.JSON.write(tidMap, Chaperone.getWorkDirectory() + "/tid.json");
  }
}
