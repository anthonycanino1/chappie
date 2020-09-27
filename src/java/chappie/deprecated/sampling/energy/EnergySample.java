package chappie.sampling.energy;

import static jrapl.util.EnergyCheckUtils.SOCKETS;

import chappie.profiling.MergableSample;
import chappie.profiling.Sample;
import chappie.profiling.TimestampedSample;
import chappie.util.TimeUtil;
import java.time.Instant;
import java.util.Arrays;
import jrapl.EnergyStats;

/** Energy values at some time accessible by socket. */
public final class EnergySample implements MergableSample<EnergySample>, TimestampedSample {
  public static final EnergySample EMPTY = new EnergySample(new double[SOCKETS], new double[SOCKETS], Instant.MAX);

  private final Instant timestamp;
  private final double[] energy = new double[SOCKETS];
  private final double[] dram = new double[SOCKETS];

  EnergySample(EnergyStats[] first, EnergyStats[] second) {
    timestamp = Instant.now();
    for (int socket = 0; socket < SOCKETS; socket++) {
      EnergyStats stats = second[socket].difference(first[socket]);
      energy[socket] = stats.getCpu() + stats.getPackage() + stats.getDram();
      dram[socket] = stats.getDram();
    }
  }

  /** private constructor to prevent mutation during merges */
  private EnergySample(double[] energy, double[] dram, Instant timestamp) {
    this.timestamp = timestamp;
    for (int socket = 0; socket < SOCKETS; socket++) {
      this.energy[socket] = energy[socket];
      this.dram[socket] = dram[socket];
    }
  }

  /** Adds the values of two energy samples and takes the greater timestamp. */
  @Override
  public EnergySample merge(EnergySample other) {
    double[] energy = new double[SOCKETS];
    double[] dram = new double[SOCKETS];
    for (int socket = 0; socket < SOCKETS; socket++) {
      energy[socket] = this.energy[socket] + other.energy[socket];
      dram[socket] = this.dram[socket] + other.dram[socket];
    }
    return new EnergySample(
      energy,
      dram,
      TimeUtil.maxBelowUpper(this.timestamp, other.timestamp));
  }

  @Override
  public Instant getTimestamp() {
    return timestamp;
  }

  /** formats to csv-like (ts,esk1,esk2,...)*/
  @Override
  public String toString() {
    return String.join(",",
      timestamp.toString(),
      String.join(",", Arrays.stream(energy)
        .mapToObj(Double::toString)
        .toArray(String[]::new)));
  }

  public double getEnergy(int socket) {
    return energy[socket];
  }

  public double getDram(int socket) {
    return dram[socket];
  }
}
