package chappie.util;

import java.util.Calendar;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.text.SimpleDateFormat;

/** Logger setup so that we can easily track events. */
public final class LoggerUtil {
  public static boolean setup() {
    try {
      // build the formatter and handlers
      SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss a z");
      Formatter formatter = new Formatter() {
        @Override
        public String format(LogRecord record) {
          String date = dateFormatter.format(new Date(record.getMillis()));
          return "chappie (" + date + ") [" + Thread.currentThread().getName() + "]: " + record.getMessage() + "\n";
        }
      };

      ConsoleHandler handler = new ConsoleHandler();
      handler.setFormatter(formatter);
      // TODO(timur): should add a file logger for transactions

      Logger logger = Logger.getLogger("chappie");
      logger.setUseParentHandlers(false);

      for (Handler hdlr: logger.getHandlers())
        logger.removeHandler(hdlr);

      logger.addHandler(handler);

      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private LoggerUtil() { }
}
