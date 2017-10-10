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

import chappie.util.*;

import java.io.*;

public class LogUtil {
  public static final String LOG_PREFIX_ENVVAR = "CHAPPIE_LOG";

  protected static String _logname = "chappie.log";

  private static PrintWriter logger = null;

  public static void initLogger() {
    String logname = System.getenv(LOG_PREFIX_ENVVAR);
    if (logname != null) {
      _logname = logname;
    }

    openLogger();
  }

  public static void openLogger() {
    try {
      switch (OsUtil.getOsType()) {
      case WINDOWS:
        System.err.println("Windows not supported. Exiting.");
        System.exit(1);
        break;
      case MACOS:
        System.err.println("Windows not supported. Exiting.");
        System.exit(1);
        break;
      case LINUX:
        logger = new PrintWriter(new BufferedWriter(new FileWriter(_logname)));
        break;
      case ANDROID:
        logger = new PrintWriter(new BufferedWriter(new FileWriter("/sdcard/stoke/" + _logname)));
        break;
      case NONE:
        System.err.println("Encountered unsupported operating system. Exiting.");
        System.exit(1);
        break;
      }
    } catch (Exception e) {
      System.err.println("Error: " + e.getMessage());
      throw new RuntimeException("uh oh");
    }
  }

  public static void closeLogger() {
    try {
      logger.close();
    } catch (Exception e) {
      System.err.println("Error: " + e.getMessage());
    }
  }

  public static void writeLogger(String msg) {
    try {
      logger.write(msg);
    } catch (Exception e) {
      System.err.println("Error: " + e.getMessage());
    }
  }

  public static void appendSuffix(int n) {
    try {
      String newName = String.format("%s.%d", _logname, n);
      new File(_logname).renameTo(new File(newName));
    } catch (Exception e) {
      System.err.println("Error: " + e.getMessage());
    }
  }
}
