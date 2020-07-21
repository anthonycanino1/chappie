package chappie.processing.trace;

import static jrapl.util.EnergyCheckUtils.SOCKETS;
import static java.util.stream.Collectors.toList;

import java.util.stream.StreamSupport;

import chappie.attribution.AttributionProfile;
import chappie.attribution.EnergyAttributer;
import chappie.attribution.EnergyAttribution;
import chappie.sampling.trace.StackTraceSample;
import chappie.sampling.trace.StackTraceSet;
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
import javax.inject.Inject;

/**
* Computes an estimate of application energy consumption from data across the
* runtime. This implements the same logic used in our data processing codebase
* (src/python/attribution) to compute runtime attribution. Data is stored in
* a typed collection and picked up by a processing method.
*/
public final class StackTraceAligner implements SampleProcessor<Iterable<Profile>> {
  private final EnergyAttributer attributer;
  private final StackTraceSorter traceData = new StackTraceSorter();

  @Inject
  StackTraceAligner(EnergyAttributer attributer) {
    this.attributer = attributer;
  }

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
  public Iterable<Profile> process() {
    ArrayList<Profile> profiles = new ArrayList<>();

    Iterator<Entry<Instant, List<StackTraceSample>>> traceIt = traceData.process().iterator();
    Entry<Instant, List<StackTraceSample>> traceEntry = null;
    StackTraceRanking.Builder ranking = new StackTraceRanking.Builder();
    for (Profile profile: attributer.process()) {
      EnergyAttribution attr = (EnergyAttribution) profile;
      for (;;) {
        if (traceEntry == null && !traceIt.hasNext()) {
          profiles.add(new AttributionProfile(attr, ranking.build()));
          ranking = new StackTraceRanking.Builder();
          break;
        } else if (traceEntry == null) {
          traceEntry = traceIt.next();
        }

        Instant timestamp = traceEntry.getKey();

        if (TimeUtil.greaterThan(timestamp, attr.getEnd())) {
          profiles.add(new AttributionProfile(attr, ranking.build()));
          ranking = new StackTraceRanking.Builder();
          break;
        } else if (TimeUtil.atLeast(timestamp, attr.getStart())) {
          List<StackTraceSample> traces = traceEntry.getValue();

          // energy assignment; injection can go here
          double energy = attr.getApplicationEnergy() / traces.size();
          for (StackTraceSample sample: traces) {
            ranking.add(sample.getStackTrace(), energy);
          }

          traceEntry = null;
        } else {
          traceEntry = null;
        }
      }
    }

    return profiles;
  }
}
