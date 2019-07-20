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

package chappie.input;

import chappie.input.Parser;

public class Config {
  public enum Mode {NOP, SLEEP, POLL, SAMPLE}

  public String workPath;
  public Mode mode = Mode.NOP;
  public int timerRate = 1;
  public boolean skipMisses = true;
  public int vmFactor = 1;
  public int hpFactor = 1;
  public int osFactor = 1;
  public int raplFactor = 1;

  Config(String workPath) { this.workPath = workPath; }
  Config(String workPath, int timerRate) { this.workPath = workPath; this.mode = Mode.SLEEP; this.timerRate = timerRate; }
  Config(String workPath, Mode mode, int timerRate, int vmFactor, int hpFactor, int osFactor, int raplFactor) {
    this.workPath = workPath;
    this.mode = mode;
    this.timerRate = timerRate;
    this.vmFactor = vmFactor;
    this.hpFactor = hpFactor;
    this.osFactor = osFactor;
    this.raplFactor = raplFactor;
  }

  public String toString() {
    String message = "\tChaperone Parameters:";
    message += "\n\t - Mode:\t\t\t" + mode;
    if (mode == Mode.SLEEP) {
      message += "\n\t - Sleep Rate:\t\t" + timerRate + " milliseconds";
    } else if (mode == Mode.POLL || mode == Mode.SAMPLE) {
      message += "\n\t - VM Polling Rate:\t\t" + vmFactor * timerRate + " milliseconds";
      message += "\n\t - HP Polling Rate:\t\t" + hpFactor * timerRate + " milliseconds";
      message += "\n\t - OS Polling Rate:\t\t" + osFactor * timerRate + " milliseconds";
      message += "\n\t - RAPL Polling Rate:\t\t" + raplFactor * timerRate + " milliseconds";
    }
    return message;
  }

  public static Config readConfig(String configPath, String workDir) {
    return new Parser(configPath, workDir).getConfig();
  }
}
