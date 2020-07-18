package chappie.sampling.energy;

import chappie.profiling.Sample;
import chappie.profiling.Sampler;
import jrapl.EnergyStats;

/** Relative sampler for energy. */
public final class EnergySampler implements Sampler<EnergySample> {
  private EnergyStats[] last;

  public EnergySampler() { last = EnergyStats.get(); }

  /** Returns the energy consumed since the last sample collected. */
  @Override
  public EnergySample sample() {
    EnergyStats[] current = EnergyStats.get();
    EnergySample sample = new EnergySample(last, current);
    last = current;

    return sample;
  }
}
