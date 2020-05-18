package chappie.attribution.processing;

import static java.lang.Math.min;
import static jrapl.util.EnergyCheckUtils.SOCKETS;

import chappie.attribution.sampling.cpu.CPUSample;
import chappie.attribution.sampling.energy.EnergySample;
import chappie.attribution.sampling.tasks.TasksSample;
import chappie.util.profiling.Profile;
import chappie.util.profiling.Sample;
import java.time.Instant;

/**
* Representation of the application's power usage. Included is the total energy
* consumed, the energy attributed to the application, and the amount of energy
* each task in the attribution should be assigned.
*/
public final class EnergyAttribution implements Profile {
  private final Instant start;
  private final Instant end;
  private final double[] total = new double[SOCKETS];
  private final double[] attributed = new double[SOCKETS];
  private final double[] taskAttributed = new double[SOCKETS];

  EnergyAttribution(
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
      taskAttributed[socket] = attributed[socket] / tasks.getTaskCount(socket);
    }
  }

  /** Returns the percent of total machine power consumed. */
  @Override
  public double evaluate() {
    double attributed = 0;
    double total = 0;
    for (int i = 0; i < SOCKETS; i++) {
      attributed += this.attributed[i];
      total += this.total[i];
    }
    if (total == 0) {
      return 0;
    } else {
      return attributed / total;
    }
  }

  /** Returns the ratio of the fractional power. */
  @Override
  public double compare(Profile other) {
    if (other instanceof EnergyAttribution) {
      return this.evaluate() / other.evaluate();
    } else {
      return 0;
    }
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
