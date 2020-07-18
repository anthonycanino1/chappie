package chappie.naive;

import static jrapl.util.EnergyCheckUtils.SOCKETS;

import chappie.attribution.EnergyAttribution;
import chappie.sampling.energy.EnergySample;
import chappie.profiling.Profile;
import java.time.Instant;

/**
* Representation of the application's power usage. Included is the total energy
* consumed, the energy attributed to the application, and the amount of energy
* each task in the attribution should be assigned.
*/
public final class NaiveEnergyAttribution implements EnergyAttribution {
  private final Instant start;
  private final Instant end;
  private final double[] energy = new double[SOCKETS];

  NaiveEnergyAttribution(
      Instant start,
      Instant end,
      EnergySample energy) {
    this.start = start;
    this.end = end;
    for (int socket = 0; socket < SOCKETS; socket++) {
      this.energy[socket] = energy.getEnergy(socket);
    }
  }

  @Override
  public double getApplicationEnergy() {
    return getTotalEnergy();
  }

  @Override
  public double getTotalEnergy() {
    double energy = 0;
    for (int i = 0; i < SOCKETS; i++) {
      energy += this.energy[i];
    }
    return energy;
  }

  @Override
  public Instant getStart() {
    return start;
  }

  @Override
  public Instant getEnd() {
    return end;
  }

  @Override
  public String toString() {
    String[] attribution = new String[SOCKETS];
    for (int socket = 0; socket < SOCKETS; socket++) {
      attribution[socket] = String.join(",",
        Long.toString(start.toEpochMilli()),
        Long.toString(end.toEpochMilli()),
        Integer.toString(socket + 1),
        Double.toString(energy[socket]),
        Double.toString(energy[socket]));
    }
    return String.join(System.lineSeparator(), attribution);
  }
}
