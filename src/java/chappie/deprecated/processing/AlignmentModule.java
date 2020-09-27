package chappie.processing;

import static java.util.Collections.unmodifiableSet;

import chappie.attribution.AttributionSampler;
import chappie.processing.trace.StackTraceAligner;
import chappie.profiling.Profile;
import chappie.profiling.SampleProcessor;
import chappie.profiling.Sampler;
import chappie.sampling.energy.EnergySampler;
import chappie.sampling.trace.StackTraceSampler;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import java.util.HashSet;
import java.util.Set;

/** Module to provide a pipeline for method alignment with energy data. */
@Module
public interface AlignmentModule {
  @Binds
  abstract SampleProcessor<Iterable<Profile>> provideProcessor(StackTraceAligner aligner);

  @Provides
  static Set<Sampler> provideSamplers(
    @AttributionSampler Set<Sampler> attributionSamplers) {
    HashSet<Sampler> samplers = new HashSet<>();
    samplers.add(new StackTraceSampler());
    samplers.addAll(attributionSamplers);
    return unmodifiableSet(samplers);
  }
}
