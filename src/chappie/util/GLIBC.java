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

import java.nio.charset.StandardCharsets;

import com.sun.jna.Library;
import com.sun.jna.Native;

public interface GLIBC extends Library {
  static GLIBC glibc = (GLIBC)Native.loadLibrary("c", GLIBC.class);
  int syscall(int number, Object... args);

  public static int getProcessId() {
    return GLIBC.glibc.syscall(39);
  }

  public static int getThreadId() {
    return GLIBC.glibc.syscall(186);
  }

  public static int getCore(int pid, int tid) {
    String path = "/proc/" + pid + "/task/" + tid + "/stat";
    int fd = GLIBC.glibc.syscall(2, path, 0);

    byte[] bytes = new byte[512];
    GLIBC.glibc.syscall(0, fd, bytes, 512);

    GLIBC.glibc.syscall(4, fd);

    String message = new String(bytes, StandardCharsets.UTF_8);
    return Integer.parseInt(message.split(" ")[38]);
  }
}
