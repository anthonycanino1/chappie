package chappie.naive;

import chappie.attribution.EnergyAttributer;
import chappie.processing.trace.StackTraceAligner;
import chappie.profiling.Profile;
import chappie.profiling.SampleProcessor;
import chappie.profiling.Sampler;
import chappie.profiling.SamplingRate;
import chappie.sampling.energy.EnergySampler;
import chappie.sampling.trace.StackTraceSampler;
import chappie.sampling.trace.AsyncProfilerRate;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

@Module
public interface NaiveAttributionModule {
  @Binds
  abstract SampleProcessor<Iterable<Profile>> provideProcessor(StackTraceAligner aligner);

  @Provides
  static EnergyAttributer provideAttributer() {
    return new NaiveEnergyAttributer();
  }

  @Provides
  @SamplingRate
  static Duration provideSamplingRate() {
    return Duration.ofMillis(4);
  }

  @Provides
  @AsyncProfilerRate
  static Duration provideAsyncRate() {
    return Duration.ofMillis(1);
  }

  @Provides
  static Set<Sampler> provideSamplers() {
    // return Set.of(new EnergySampler(), new StackTraceSampler());
    return Set.of(new EnergySampler());
  }

  @Provides
  static ExecutorService provideExecutor(Set<Sampler> samplers) {
    final AtomicInteger counter = new AtomicInteger();
    return Executors.newFixedThreadPool(
      samplers.size(), r -> new Thread(
      r,
      String.join("-",
        "chappie",
        String.format("%02d", counter.getAndIncrement()))));
  }
}
