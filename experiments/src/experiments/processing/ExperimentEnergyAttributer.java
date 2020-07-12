package experiments.processing;

import static jrapl.util.EnergyCheckUtils.SOCKETS;

import chappie.attribution.EnergyAttributer;
import chappie.profiling.Sample;
import chappie.profiling.TimestampedSample;
import java.time.Instant;
import java.util.ArrayList;
import java.util.TreeMap;

/**
* Computes an estimate of application energy consumption from data across the
* runtime. This implements the same logic used in our data processing codebase
* (src/python/attribution) to compute runtime attribution. Data is stored in
* a typed collection and picked up by a processing method.
*/
public final class ExperimentEnergyAttributer implements EnergyAttributer<ExperimentEnergyAttribution> {
  private final TreeMap<Instant, ExperimentAligner> data = new TreeMap<>();

  /** Puts the data in relative timestamp-indexed storage to keep things sorted. */
  @Override
  public void add(Sample s) {
    if (s instanceof TimestampedSample) {
      synchronized(this) {
        Instant timestamp = ((TimestampedSample) s).getTimestamp();
        data.putIfAbsent(timestamp, new ExperimentAligner());
        data.get(timestamp).add(s);
      }
    }
  }

  @Override
  public Iterable<ExperimentEnergyAttribution> process() {
    int attempts = 0;
    ArrayList<ExperimentEnergyAttribution> attributions = new ArrayList<>();
    ExperimentAligner aligner = new ExperimentAligner();
    synchronized (this) {
      for (Instant timestamp: data.keySet()) {
        aligner = aligner.merge(data.get(timestamp));
        // if (aligner.valid()) {
        if (aligner.valid() || (attempts++ >= 10 && aligner.attributable())) {
          attributions.add(aligner.process());
          aligner = new ExperimentAligner();
          attempts = 0;
        }
      }
      data.clear();
      // data.put(aligner.getTimestamp(), aligner);
      if (aligner.attributable()) {
        attributions.add(aligner.process());
      } else {
        data.put(aligner.getTimestamp(), aligner);
      }
    }
    return attributions;
  }
}
