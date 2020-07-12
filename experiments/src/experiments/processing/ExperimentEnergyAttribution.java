package experiments.processing;

import static jrapl.util.EnergyCheckUtils.SOCKETS;

import chappie.attribution.EnergyAttribution;
import chappie.attribution.sampling.energy.EnergySample;
import experiments.sampling.cpu.CPUSample;
import experiments.sampling.tasks.TasksSample;
import java.time.Instant;

/**
* Representation of the application's power usage. Included is the total energy
* consumed, the energy attributed to the application, and the amount of energy
* each task in the attribution should be assigned.
*/
public final class ExperimentEnergyAttribution implements EnergyAttribution {
  private final Instant start;
  private final Instant end;
  private final double[] total = new double[SOCKETS];
  private final double[] attributed = new double[SOCKETS];

  ExperimentEnergyAttribution(
      Instant start,
      Instant end,
      EnergySample energy,
      CPUSample cpu,
      TasksSample tasks) {
    this.start = start;
    this.end = end;
    for (int socket = 0; socket < SOCKETS; socket++) {
      // compute the attribution factor
      double factor = 0;
      if (cpu.getJiffies(socket) > 0) {
        factor = (double)(Math.min(tasks.getJiffies(socket), cpu.getJiffies(socket))) / cpu.getJiffies(socket);
      }
      // assign the energy
      total[socket] = energy.getEnergy(socket);
      attributed[socket] = factor * total[socket];
    }
  }

  @Override
  public double getApplicationEnergy() {
    double energy = 0;
    for (int i = 0; i < SOCKETS; i++) {
      energy += this.attributed[i];
    }
    return energy;
  }

  @Override
  public double getTotalEnergy() {
    double energy = 0;
    for (int i = 0; i < SOCKETS; i++) {
      energy += this.total[i];
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
  public String dump() {
    String[] attribution = new String[SOCKETS];
    for (int socket = 0; socket < SOCKETS; socket++) {
      attribution[socket] = String.join(",",
        Long.toString(start.toEpochMilli()),
        Long.toString(end.toEpochMilli()),
        Integer.toString(socket + 1),
        Double.toString(total[socket]),
        Double.toString(attributed[socket]));
    }
    return String.join(System.lineSeparator(), attribution);
  }

  @Override
  public String toString() {
    String[] attribution = new String[SOCKETS];
    for (int socket = 0; socket < SOCKETS; socket++) {
      attribution[socket] =
        "socket " + (socket + 1) + " - "
        + String.format("%.2f", attributed[socket]) + "J/"
        + String.format("%.2f", total[socket]) + "J";
    }

    String message = String.join(System.lineSeparator(), attribution);
    return String.join(
      System.lineSeparator(),
      "attribution from " + start + " to " + end,
      message);
  }
}
