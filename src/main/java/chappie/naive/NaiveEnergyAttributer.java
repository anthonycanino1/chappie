package chappie.naive;

import chappie.attribution.EnergyAttributer;
import chappie.attribution.EnergyAttribution;
import chappie.sampling.energy.EnergySample;
import chappie.profiling.Sample;
import java.time.Instant;
import java.util.ArrayList;

final class NaiveEnergyAttributer implements EnergyAttributer {
  private Instant lastTimestamp;
  private ArrayList<EnergyAttribution> attributions = new ArrayList<>();

  public NaiveEnergyAttributer() {
    lastTimestamp = Instant.now();
  }

  /** Puts the data in relative timestamp-indexed storage to keep things sorted. */
  @Override
  public void add(Sample s) {
    if (s instanceof EnergySample) {
      EnergySample sample = (EnergySample) s;
      synchronized (this) {
        attributions.add(new NaiveEnergyAttribution(lastTimestamp, sample.getTimestamp(), sample));
        lastTimestamp = sample.getTimestamp();
      }
    }
  }

  @Override
  public Iterable<EnergyAttribution> process() {
    ArrayList<EnergyAttribution> attributions;
    synchronized (this) {
       attributions = this.attributions;
       this.attributions = new ArrayList();
     }
    return attributions;
  }
}
