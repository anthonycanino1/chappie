package chappie.attribution;

import chappie.attribution.processing.AttributionProcessor;
import chappie.attribution.processing.EnergyAttribution;
import chappie.attribution.sampling.cpu.CPUSampler;
import chappie.attribution.sampling.energy.EnergySampler;
import chappie.attribution.sampling.tasks.TasksSampler;
import chappie.util.concurrent.SelfScheduledRunnable;
import chappie.util.concurrent.RunnableCollectionExecutor;
import chappie.util.logging.ChappieLogger;
import chappie.util.profiling.Profile;
import chappie.util.profiling.Profiler;
import chappie.util.profiling.Sampler;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import chappie.util.profiling.Sample;

/**
* Manages a system that computes an attribution profile; i.e., an estimate of
* application energy consumption with varying degrees of abstraction. In
* addition to managing underlying threads, the profiler also provides relative
* reporting.
*/
public final class AttributionProfiler implements Profiler {
  private final RunnableCollectionExecutor executor = new RunnableCollectionExecutor();
  private final AttributionProcessor processor = new AttributionProcessor();

  private int lastProfileIndex = 0;

  public AttributionProfiler(int samplingRate, int processingRate) {
    ArrayList<SelfScheduledRunnable> runnables = new ArrayList<>();

    Sampler[] samplers = new Sampler[] {
      new EnergySampler(),
      new TasksSampler(),
      new CPUSampler(),
      // new StackTraceSampler()
    };

    for (Sampler sampler: samplers) {
      runnables.add(new SelfScheduledRunnable(
        () -> processor.add(sampler.sample()),
        samplingRate));
    }

    // runnables.add(new SelfScheduledRunnable(processor::process, processingRate));

    for (SelfScheduledRunnable runnable: runnables) {
      executor.add(runnable);
    }

    final AtomicInteger counter = new AtomicInteger();
    executor.setThreadFactory(r -> new Thread(
      r,
      String.join(
        "-",
        "chappie",
        "attr",
        String.format("%02d", counter.getAndIncrement()))));
  }

  @Override
  public void start() {
    executor.start();
  }

  @Override
  public void stop() {
    executor.stop();
  }

  @Override
  public Iterable<Profile> getProfiles() {
    ArrayList<Profile> attributions = new ArrayList<>();
    EnergyAttribution attribution = processor.process();
    while (attribution != null) {
      attributions.add(attribution);
      attribution = processor.process();
    }
    return attributions;
  }
}
