package chappie.naive;

import chappie.attribution.AttributionSampler;
import chappie.attribution.EnergyAttributer;
import chappie.profiling.Sampler;
import chappie.profiling.SamplingRate;
import chappie.sampling.energy.EnergySampler;
import dagger.Module;
import dagger.Provides;
import java.time.Duration;
import java.util.Set;

/** Module to provide naive energy attribution (jRAPL only). */
@Module
public interface NaiveEnergyModule {
  @Provides
  static EnergyAttributer provideAttributer() {
    return new NaiveEnergyAttributer();
  }

  @Provides
  @SamplingRate
  static Duration provideSamplingRate() {
    return Duration.ofMillis(Long.parseLong(System.getProperty("chappie.rate", "4")));
  }

  @Provides
  @AttributionSampler
  static Set<Sampler> provideAttributionSamplers() {
    return Set.of(new EnergySampler());
  }
}
