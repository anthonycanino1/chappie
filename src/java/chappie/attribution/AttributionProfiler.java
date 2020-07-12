package chappie.attribution;

import static java.util.logging.Level.WARNING;

import chappie.attribution.processing.NaiveStackTraceAligner;
import chappie.attribution.sampling.energy.EnergySampler;
import chappie.attribution.sampling.trace.StackTraceSampler;
import chappie.concurrent.SelfScheduledRunnable;
import chappie.concurrent.RunnableCollectionExecutor;
import chappie.profiling.Profile;
import chappie.profiling.Profiler;
import chappie.profiling.Sampler;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import chappie.util.LoggerUtil;
import java.util.logging.Logger;

/**
* Manages a system that computes an attribution profile; i.e., an estimate of
* application energy consumption with varying degrees of abstraction.
*/
public final class AttributionProfiler implements Profiler<AttributionProfile> {
  private final Logger logger;
  private final RunnableCollectionExecutor samplerExecutor = new RunnableCollectionExecutor();
  private final NaiveStackTraceAligner aligner = new NaiveStackTraceAligner();
  private final ArrayList<AttributionProfile> profiles = new ArrayList<>();

  public AttributionProfiler(int rate) {
    logger = Logger.getLogger("chappie");
    logger.info("building attribution profiler");
    Sampler[] samplers = new Sampler[] {
      new EnergySampler(),
      // new TasksSampler(),
      // new CPUSampler()//,
      new StackTraceSampler()
    };

    for (Sampler sampler: samplers) {
      samplerExecutor.add(new SelfScheduledRunnable(
        () -> {
          // String[] samplerPackage = sampler.getClass().getName().split("\\.");
          // String samplerClass = samplerPackage[samplerPackage.length - 1];
          try {
            // logger.info("collecting from " + samplerClass);
            aligner.add(sampler.sample());
          } catch (RuntimeException e) {
            logger.log(WARNING, "unable to collect", e);
          }
        },
        rate));
    }

    final AtomicInteger counter = new AtomicInteger();
    samplerExecutor.setThreadFactory(r -> new Thread(
      r,
      String.join(
        "-",
        "chappie",
        "attr",
        String.format("%02d", counter.getAndIncrement()))));
  }

  @Override
  public void start() {
    samplerExecutor.start();
  }

  @Override
  public void stop() {
    samplerExecutor.stop();
  }

  /** Processes the data and adds any profiles produced to the underlying storage. */
  @Override
  public Iterable<AttributionProfile> getProfiles() {
    int count = 0;
    for (AttributionProfile profile: aligner.process()) {
      profiles.add(profile);
      count++;
    }
    if (count > 0) {
      logger.info("produced " + count + " profiles");
    }
    return profiles;
  }
}
