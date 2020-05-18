/* ************************************************************************************************
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * Copyright 2017 SUNY Binghamton
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

import chappie.Chappie;
import chappie.util.profiling.Profile;
import chappie.util.logging.ChappieLogger;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

public class Driver {
  public static void main(String[] args) {
    ChappieLogger.setup();
    Logger logger = Logger.getLogger("chappie");

    Chappie.start();
    for (int i = 0; i < 1; i++) {
      long start = System.currentTimeMillis();
      try {
        Thread.sleep(2500);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    Chappie.stop();

    try (FileWriter fw = new FileWriter("chappie-logs/log.txt")) {
      PrintWriter writer = new PrintWriter(fw);
      writer.println("start,end,socket,total,attributed");
      for (Profile profile: Chappie.getProfiles()) {
        writer.println(profile.dump());
      }
    } catch (Exception e) {
      logger.info("couldn't open log file");
    }
  }
}
