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

package chappie;

import static java.util.concurrent.TimeUnit.SECONDS;

import chappie.profile.processing.EnergyProfile;
import chappie.profile.processing.TraceProfile;
import chappie.profile.processing.Attributer;
import chappie.profile.Profiler;
import chappie.profile.Record;
import chappie.profile.sampling.*;
import chappie.util.Histogram;
import chappie.util.LoggerUtil;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import jlibc.proc.CPU;

import java.nio.file.Paths;
import java.nio.file.Files;

public final class Chaperone {
  private static final AtomicInteger counter = new AtomicInteger();
  private static final int rate = Integer.parseInt(System.getProperty("chappie.rate", "32"));
  private static final int freqRate = Integer.parseInt(System.getProperty("chappie.freq_rate", "512"));

  private static Chaperone chappie;

  private final int id = counter.getAndIncrement();
  private final Logger logger = Logger.getLogger("chappie-" + id);
  private final String workDirectory = System.getProperty("chappie.dir", "chappie-logs");

  private final ArrayList<Profiler> profilers = new ArrayList<Profiler>();
  private ExecutorService executor;

  private final ArrayList<Histogram> freqs = new ArrayList<>();
  private final Attributer attributer = new Attributer();

  private static Chaperone.Mode mode;

  private double energy;

  private enum Mode {
    FREQ(
      Profiler.buildProfiler(512, () -> chappie.freqs.add(new Histogram(CPU.getFreqs(), 120, 310, 19)))
    ),
    PROF(
      Profiler.buildProfiler(freqRate, () -> chappie.freqs.add(new Histogram(CPU.getFreqs(), 120, 310, 19))),
      Profiler.buildProfiler(rate, () -> chappie.attributer.add(RAPLSampler.sample())),
      Profiler.buildProfiler(rate, () -> chappie.attributer.add(TaskSampler.sample())),
      Profiler.buildProfiler(rate, () -> chappie.attributer.add(CPUSampler.sample())),
      Profiler.buildProfiler(rate, () -> chappie.attributer.add(TraceSampler.sample())),
      Profiler.buildProfiler(4, () -> {
        ArrayList<TraceProfile> profiles = chappie.attributer.attribute();
        if (profiles.size() > 0) {
          System.out.println(profiles);
        }
        // for (TraceProfile profile: profiles) {
          // chappie.logger.info(profile.toString());
          // if (profile.getEnergy() > 0) {
            // chappie.energy += profile.getEnergy();
          // }
        // }
      })
    );

    private final ArrayList<Profiler> profilers = new ArrayList<Profiler>();
    Mode(Profiler ...profilers) {
      for (Profiler profiler: profilers)
        this.profilers.add(profiler);
    }

    public ArrayList<Profiler> profilers() {
      // make a defensive copy so no one tries to modify the enum field
      ArrayList<Profiler> profilers = new ArrayList<Profiler>();
      for (Profiler profiler: this.profilers)
        profilers.add(profiler);

      return profilers;
    }
  }

  public static void start() {
    mode = Chaperone.Mode.valueOf(
      System.getProperty("chappie.mode", "PROF").toUpperCase());

    chappie = new Chaperone();
    for (Profiler profiler: mode.profilers())
      chappie.addProfiler(profiler);
    chappie.startProfiling();
  }

  public static void stop() {
    chappie.stopProfiling();
    chappie = null;
  }

  private Chaperone() {
    LoggerUtil.setupLogger(id);
  }

  private void startProfiling() {
    final AtomicInteger counter = new AtomicInteger();
    executor = Executors.newFixedThreadPool(profilers.size(),
      r -> new Thread(r, "chappie-" + id + "-" + counter.getAndIncrement()));
    for (Profiler profiler: profilers) {
      executor.submit(profiler);
    }
  }

  private void stopProfiling() {
    executor.shutdownNow();
    try {
      while (!executor.awaitTermination(1, SECONDS)) {}
    } catch (Exception e) { }

    logger.info("energy consumed: " + String.format("%.02f", energy) + " J");

    // logger.info(attributer.toString());

    // try {
    //   Files.write(Paths.get("temp/chappie-" + id + "-" + mode), chappie.freqs.toString().getBytes());
    // } catch(Exception e) { }
  }

  private void addProfiler(Profiler profiler) {
    profilers.add(profiler);
  }
}
