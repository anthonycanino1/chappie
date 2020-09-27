package chappie.attribution;

import chappie.processing.trace.StackTraceRanking;
import chappie.profiling.Profile;
import java.lang.Math;
import java.util.Arrays;
import java.util.List;

/**
* Representation of an application's power usage. Included is the total energy
* consumed, the energy attributed to the application, and the relative
* energy consumption of sampled methods in some interval.
*/
public final class AttributionProfile implements Profile {

  private final EnergyAttribution attribution;
  private final StackTraceRanking ranking;

  public AttributionProfile(EnergyAttribution attribution, StackTraceRanking ranking) {
    this.attribution = attribution;
    this.ranking = ranking;
  }

  @Override
  public String toString() {
    return String.join(System.lineSeparator(),
      "energy attribution:",
      attribution.toString(),
      "stack trace ranking:",
      ranking.toString());
  }

  public EnergyAttribution getAttribution() {
    return attribution;
  }

  public StackTraceRanking getRanking() {
    return ranking;
  }
}
