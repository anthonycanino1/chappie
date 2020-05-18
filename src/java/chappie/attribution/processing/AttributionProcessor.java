package chappie.attribution.processing;

import static jrapl.util.EnergyCheckUtils.SOCKETS;

import chappie.attribution.sampling.cpu.CPUSample;
import chappie.attribution.sampling.energy.EnergySample;
import chappie.attribution.sampling.tasks.TasksSample;
import chappie.util.profiling.Profile;
import chappie.util.profiling.Sample;
import chappie.util.profiling.SampleProcessor;
import java.time.Instant;
import java.util.ArrayList;
import java.util.TreeMap;

/**
* Computes an estimate of application energy consumption from data across the
* runtime. This implements the same logic used in our data processing codebase
* (src/python/attribution) to compute runtime attribution. Data is stored in
* a typed collection and picked up by a processing method.
*/
public final class AttributionProcessor implements SampleProcessor<EnergyAttribution> {
  private final TreeMap<Instant, EnergyAttributer> rawData = new TreeMap<>();
  private final ArrayList<EnergyAttribution> attributions = new ArrayList<>();

  /** Puts the data in relative timestamp-indexed storage to keep things sorted. */
  @Override
  public void add(Sample s) {
    if (s.getTimestamp() != null) {
      rawData.putIfAbsent(s.getTimestamp(), new EnergyAttributer());
      rawData.get(s.getTimestamp()).add(s);
    }
  }

  @Override
  public EnergyAttribution process() {
    attribute();
    if (!attributions.isEmpty()) {
      EnergyAttribution attribution = attributions.get(0);
      attributions.remove(0);
      return attribution;
    } else {
      return null;
    }
  }

  /**
  * Goes through the data, forward aggregating time-aligned sample groups until
  * a valid attribution can be made. All consumed data is cleared after
  * attribution.
  */
  private void attribute() {
    int attempts = 0;
    EnergyAttributer attributer = new EnergyAttributer();
    for (Instant timestamp: rawData.keySet()) {
      attributer = attributer.merge(rawData.getOrDefault(timestamp, new EnergyAttributer()));
      
      if (attributer.valid()) { // } || (attempts++ >= 5 && attributer.attributable())) {
        attributions.add((EnergyAttribution) attributer.process());
        attributer = new EnergyAttributer();
      }
    }

    rawData.clear();
    if (attributer.attributable()) {
      attributions.add(attributer.process());
    } else if (attributer.getTimestamp() != null) {
      rawData.putIfAbsent(attributer.getTimestamp(), attributer);
    }
  }
}
