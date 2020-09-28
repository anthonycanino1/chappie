package chappie;

import clerk.Processor;
import chappie.util.TimeUtil;
import eflect.EflectProcessor;
import eflect.EnergyFootprint;
import eflect.data.Sample;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import javax.inject.Inject;
/**
* Computes an estimate of application energy consumption from data across the
* runtime. This implements the same logic used in our data processing codebase
* (src/python/attribution) to compute runtime attribution. Data is stored in
* a typed collection and picked up by a processing method.
*/
public final class StackTraceRanker implements Processor<Object, Map<String, Double>> {
  private final EflectProcessor eflect = new EflectProcessor();

  private TreeMap<Instant, ArrayList<String>> traces = new TreeMap<>();

  /** Puts the data in relative timestamp-indexed storage to keep things sorted. */
  @Override
  public void accept(Object o) {
    if (o instanceof String) {
      for (String record: ((String) o).split("\n")) {
        String[] values = record.split(",");
        Instant timestamp = Instant.ofEpochMilli(Long.parseLong(values[0]));
        String trace = values[2];
        synchronized (this.traces) {
          this.traces.putIfAbsent(timestamp, new ArrayList<String>());
          this.traces.get(timestamp).add(trace);
        }
      }
    } else if (o instanceof Sample) {
      eflect.accept((Sample) o);
    }
  }

  @Override
  public Map<String, Double> get() {
    HashMap<String, Double> rankings = new HashMap<>();

    TreeMap<Instant, ArrayList<String>> traces = this.traces;
    synchronized(this.traces) {
      this.traces = new TreeMap<>();
    }
    Iterator<Instant> timestamps = traces.keySet().iterator();
    Instant timestamp = Instant.EPOCH;
    for (EnergyFootprint footprint: eflect.get()) {
      if (TimeUtil.isEpoch(timestamp)) {
        if (timestamps.hasNext()) {
          timestamp = timestamps.next();
        } else {
          break;
        }
      }

      if (TimeUtil.atLeast(timestamp, footprint.getStart())
          && TimeUtil.atMost(timestamp, footprint.getEnd())) {
        ArrayList<String> methods = traces.get(timestamp);
        double energy = footprint.getEnergy() / methods.size();
        for (String method: methods) {
          rankings.put(method, rankings.getOrDefault(method, 0.0) + energy);
        }
        timestamp = Instant.EPOCH;
      }
    }

    return rankings;
  }
}
