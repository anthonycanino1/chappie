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

package chappie.util;

import java.util.Calendar;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.text.SimpleDateFormat;

public final class LoggerUtil {
  private static Logger logger;

  public static void setupLogger(int id) {
    // build the formatter and handlers
    SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss a z");
    Formatter formatter = new Formatter() {
      @Override
      public String format(LogRecord record) {
        String date = dateFormatter.format(new Date(record.getMillis()));
        return "chappie-" + id + " (" + date + "): " + record.getMessage() + "\n";
      }
    };

    ConsoleHandler handler = new ConsoleHandler();
    handler.setFormatter(formatter);
    // TODO(timur): should add a file logger for transactions

    // grab the current chappie logger, clean it up, and add our handlers
    logger = Logger.getLogger("chappie-" + id);

    logger.setUseParentHandlers(false);
    for (Handler hdlr: logger.getHandlers())
      logger.removeHandler(hdlr);

    logger.addHandler(handler);
  }

  public static Logger getLogger() {
    return logger;
  }

  private LoggerUtil() { }
}
