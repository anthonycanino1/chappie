package experiments;

import static java.util.logging.Level.WARNING;

import chappie.attribution.sampling.energy.EnergySampler;
import chappie.concurrent.SelfScheduledRunnable;
import chappie.concurrent.RunnableCollectionExecutor;
import chappie.profiling.Profiler;
import chappie.profiling.Sampler;
import experiments.processing.ExperimentEnergyAttributer;
import experiments.processing.ExperimentEnergyAttribution;
import experiments.sampling.cpu.CPUSampler;
import experiments.sampling.tasks.TasksSampler;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
* Manages a system that computes an attribution profile; i.e., an estimate of
* application energy consumption with varying degrees of abstraction.
*/
public final class ExperimentProfiler implements Profiler<ExperimentEnergyAttribution> {
  private final Logger logger = Logger.getLogger("chappie");
  private final RunnableCollectionExecutor samplerExecutor = new RunnableCollectionExecutor();
  private final ExperimentEnergyAttributer attributer = new ExperimentEnergyAttributer();
  private final ArrayList<ExperimentEnergyAttribution> profiles = new ArrayList<>();

  public ExperimentProfiler(int rate) {
    logger.info("building attribution profiler");
    Sampler[] samplers = new Sampler[] {
      new EnergySampler(),
      new TasksSampler(),
      new CPUSampler()
    };

    for (Sampler sampler: samplers) {
      samplerExecutor.add(new SelfScheduledRunnable(
        () -> {
          String[] samplerPackage = sampler.getClass().getName().split("\\.");
          String samplerClass = samplerPackage[samplerPackage.length - 1];
          try {
            // logger.info("collecting from " + samplerClass);
            attributer.add(sampler.sample());
          } catch (RuntimeException e) {
            logger.log(WARNING, "unable to collect from " + samplerClass, e);
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
    logger.info("starting attribution profiler");
    samplerExecutor.start();
  }

  @Override
  public void stop() {
    logger.info("stopping attribution profiler");
    samplerExecutor.stop();
  }

  /** Processes the data and adds any profiles produced to the underlying storage. */
  @Override
  public Iterable<ExperimentEnergyAttribution> getProfiles() {
    int count = 0;
    for (ExperimentEnergyAttribution profile: attributer.process()) {
      profiles.add(profile);
      count++;
    }
    if (count > 0) {
      logger.info("produced " + count + " profiles");
    }
    return profiles;
  }
}
