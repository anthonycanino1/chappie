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

import chappie.monitor.JDK9Monitor;
import chappie.util.GLIBC;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class Chaperone extends TimerTask {
  public enum Mode {NOP, SAMPLE}

  int mainID;

  // Chaperone Parameters
  private Mode mode;
  private int polling;

  // Metrics
  private int epoch;
  private long start;

  // Polling Driver
  private Timer timer;

  // Polling Monitor
  // this is the wrong name...what is it?
  private JDK9Monitor monitor;

  public Chaperone(Mode mode, int polling) {
    mainID = GLIBC.getProcessId();

    this.mode = mode;
    this.polling = polling;

    this.monitor = new JDK9Monitor();

    if (mode != Mode.NOP) {
      timer = new Timer("Chaperone");
      timer.scheduleAtFixedRate(this, 0, polling);
    }

    epoch = 0;
    start = System.nanoTime();
  }

  // Runtime data containers
  // the other containers are in the monitor now
  private ArrayList<Double> activeness = new ArrayList<Double>();

  // TIMER TASK CLASS METHODS

  // Termination flags
  // The timer class does not have a synchronized termination method. Luckily,
  // only main touches the chaperone, so we can use a double flag psuedo-lock.
  private boolean terminate = false;
  private boolean terminated = false;

  private long elapsedTime = 0;

  @Override
  public void run() {
    // Check if we need to stop
    if (!terminate) {
      // set this epoch and cache last epoch's nano timestamp
      long lastEpochTime = elapsedTime;
      elapsedTime = System.nanoTime() - start;
      lastEpochTime = elapsedTime - lastEpochTime;

      // this is how we actually read
      monitor.read(epoch);

      // estimate of chappie's machine time usage;
      // possible to use state to make better estimate
      long chappieReadingTime = (System.nanoTime() - start) - elapsedTime;
      activeness.add((double)chappieReadingTime / lastEpochTime);

      epoch++;
    } else {
      // stop ourselves before letting everything know we're done
      timer.cancel();
      terminated = true;
    }
  }

  @Override
  public boolean cancel() {
    if (mode != Mode.NOP) {
      // use the double flag to kill the process from in here
      terminate = true;
      while(!terminated) {
        try {
          Thread.sleep(0, 100);
        } catch(Exception e) { }
      }
    }

    // write the output from the monitor
    monitor.dump(start, activeness);

    return true;
  }

  // public read()
}
