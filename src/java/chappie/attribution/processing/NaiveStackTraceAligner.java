package chappie.attribution.processing;

import static jrapl.util.EnergyCheckUtils.SOCKETS;

import chappie.attribution.AttributionProfile;
import chappie.attribution.StackTraceRanking;
import chappie.attribution.processing.NaiveEnergyAttributer;
import chappie.attribution.processing.StackTraceSorter;
import chappie.attribution.sampling.trace.StackTraceSample;
import chappie.attribution.sampling.trace.StackTraceSet;
import chappie.profiling.Profile;
import chappie.profiling.Sample;
import chappie.profiling.SampleProcessor;
import chappie.util.TimeUtil;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
* Computes an estimate of application energy consumption from data across the
* runtime. This implements the same logic used in our data processing codebase
* (src/python/attribution) to compute runtime attribution. Data is stored in
* a typed collection and picked up by a processing method.
*/
public final class NaiveStackTraceAligner implements SampleProcessor<Iterable<AttributionProfile>> {
  private final NaiveEnergyAttributer attributer = new NaiveEnergyAttributer();
  private final StackTraceSorter traceData = new StackTraceSorter();

  public NaiveStackTraceAligner() { }

  /** Puts the data in relative timestamp-indexed storage to keep things sorted. */
  @Override
  public void add(Sample s) {
    if (s instanceof StackTraceSet) {
      traceData.add(s);
    } else {
      attributer.add(s);
    }
  }

  @Override
  public Iterable<AttributionProfile> process() {
    ArrayList<AttributionProfile> profiles = new ArrayList<>();

    Iterator<Entry<Instant, List<StackTraceSample>>> traceIt = traceData.process().iterator();
    Entry<Instant, List<StackTraceSample>> traceEntry = null;
    RankingBuilder ranking = new RankingBuilder();
    for (NaiveEnergyAttribution attr: attributer.process()) {
      for (;;) {
        if (traceEntry == null && !traceIt.hasNext()) {
          profiles.add(new AttributionProfile(attr, ranking.buildRanking()));
          ranking = new RankingBuilder();
          break;
        } else if (traceEntry == null) {
          traceEntry = traceIt.next();
        }

        Instant timestamp = traceEntry.getKey();

        if (TimeUtil.greaterThan(timestamp, attr.getEnd())) {
          profiles.add(new AttributionProfile(attr, ranking.buildRanking()));
          ranking = new RankingBuilder();
          break;
        } else if (TimeUtil.atLeast(timestamp, attr.getStart())) {
          List<StackTraceSample> traces = traceEntry.getValue();
          double energy = attr.getApplicationEnergy() / traces.size();
          for (StackTraceSample sample: traces) {
            ranking.add(sample.getStackTrace(), energy);
          }
          traceEntry = null;
        }
      }
    }

    return profiles;
  }
}
