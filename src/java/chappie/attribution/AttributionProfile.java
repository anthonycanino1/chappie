package chappie.attribution;

import chappie.attribution.EnergyAttribution;
import chappie.attribution.StackTraceRanking;
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

  /** Returns the total energy consumed by the application. */
  @Override
  public double evaluate() {
    return attribution.getApplicationEnergy();
  }

  /** Returns the pcc with the other profile, assuming it's an
   *  AttributionProfile or StackTraceRanking; otherwise, return NaN.
   */
  @Override
  public double compare(Profile other) {
    if (other instanceof AttributionProfile) {
      return this.ranking.compare(((AttributionProfile) other).ranking);
    } else {
      return Double.NaN;
    }
  }

  @Override
  public String dump() {
    return attribution.dump();
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
