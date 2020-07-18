package chappie.profiling;

import static java.util.logging.Level.WARNING;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import chappie.concurrent.SelfScheduledRunnable;
import chappie.profiling.Profile;
import chappie.profiling.Sampler;
import chappie.profiling.SampleProcessor;
import chappie.util.LoggerUtil;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Set;
import java.util.logging.Logger;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;

/**
* Manages a system that computes an attribution profile; i.e., an estimate of
* application energy consumption with varying degrees of abstraction.
*/
public final class Profiler {
  private static final Logger logger = LoggerUtil.setup();

  private final Duration rate;
  private final Set<Sampler> samplers;
  private final ExecutorService executor;
  private final SampleProcessor<Iterable<Profile>> processor;
  private final ArrayList<Profile> profiles = new ArrayList<>();

  private boolean isRunning = false;

  @Inject
  Profiler(@SamplingRate Duration rate, Set<Sampler> samplers, SampleProcessor<Iterable<Profile>> processor, ExecutorService executor) {
    this.rate = rate;
    this.samplers = samplers;
    this.processor = processor;
    this.executor = executor;
  }

  public void start() {
    if (!isRunning) {
      logger.info("starting the profiler");
      for (Sampler sampler: samplers) {
        executor.execute(
          new SelfScheduledRunnable(
            () -> {
              try {
                logger.finest("collecting from " + sampler.getClass().getSimpleName());
                processor.add(sampler.sample());
              } catch (RuntimeException e) {
                logger.log(WARNING, "unable to collect from " + sampler.getClass().getSimpleName(), e);
                e.printStackTrace();
              }
            },
            rate.toMillis())
        );
        logger.fine("started the " + sampler.getClass().getSimpleName());
      }
      isRunning = true;
      logger.info("started the profiler");
    } else {
      logger.warning("profiler already running");
    }
  }

  public void stop() {
    if (isRunning) {
      logger.info("stopping the profiler");
      try {
        executor.shutdownNow();
        while (!executor.awaitTermination(250, MILLISECONDS)) { }
        isRunning = false;
      } catch (Exception e) {
        logger.log(WARNING, "unable to terminate executor", e);
      }
      logger.info("stopped the profiler");
    } else {
      logger.warning("profiler not currently running");
    }
  }

  /** Processes the data and adds any profiles produced to the underlying storage. */
  public Iterable<Profile> getProfiles() {
    int count = 0;
    for (Profile profile: processor.process()) {
      profiles.add(profile);
      count++;
    }
    if (count > 0) {
      logger.finest("collected " + count + " profiles from " + processor.getClass().getSimpleName());
    }
    return profiles;
  }
}
