package chappie.processing;

import chappie.attribution.EnergyAttributer;
import chappie.attribution.AttributionSampler;
import chappie.profiling.Profile;
import chappie.profiling.SampleProcessor;
import chappie.profiling.Sampler;
import dagger.Module;
import dagger.Provides;
import java.util.Set;

/** Module that propagates the energy module. */
@Module
public interface AttributionModule {
  @Provides
  static SampleProcessor<Iterable<Profile>> provideProcessor(EnergyAttributer attributer) {
    return attributer;
  }

  @Provides
  static Set<Sampler> provideSamplers(
    @AttributionSampler Set<Sampler> attributionSamplers) {
    return attributionSamplers;
  }
}
