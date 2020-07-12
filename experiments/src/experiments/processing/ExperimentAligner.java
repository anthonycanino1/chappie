package experiments.processing;

import static jrapl.util.EnergyCheckUtils.SOCKETS;

import chappie.attribution.sampling.energy.EnergySample;
import chappie.profiling.MergableSample;
import chappie.profiling.Sample;
import chappie.profiling.SampleProcessor;
import chappie.profiling.TimestampedSample;
import chappie.util.TimeUtil;
import experiments.sampling.cpu.CPUSample;
import experiments.sampling.tasks.TasksSample;
import java.time.Instant;

final class ExperimentAligner implements SampleProcessor<ExperimentEnergyAttribution> {
  private Instant start = Instant.MAX;
  private Instant end = Instant.MIN;
  private CPUSample cpu = CPUSample.EMPTY;
  private EnergySample energy = EnergySample.EMPTY;
  private TasksSample tasks = TasksSample.EMPTY;

  @Override
  public void add(Sample s) {
    if (s instanceof EnergySample) {
      this.energy = ((EnergySample) s).merge(this.energy);
    } else if (s instanceof TasksSample) {
      this.tasks = ((TasksSample) s).merge(this.tasks);
    } else if (s instanceof CPUSample) {
      this.cpu = ((CPUSample) s).merge(this.cpu);
    } else {
      return; // prevents other timestamped from touching
    }

    Instant timestamp = ((TimestampedSample) s).getTimestamp();
    start = TimeUtil.min(timestamp, start);
    end = TimeUtil.maxBelowUpper(timestamp, end);
  }

  @Override
  public ExperimentEnergyAttribution process() {
    return new ExperimentEnergyAttribution(start, end, energy, cpu, tasks);
  }

  @Override
  public String toString() {
    return String.join("\n",
      start.toString() + "->" + end.toString(),
      energy.toString(),
      cpu.toString(),
      tasks.toString());
  }

  ExperimentAligner merge(ExperimentAligner other) {
    ExperimentAligner merged = new ExperimentAligner();
    // TODO(timur): these work for the wrong reason. need to decide on generic
    // merge...
    merged.add(other.cpu.merge(this.cpu));
    merged.add(other.energy.merge(this.energy));
    merged.add(other.tasks.merge(this.tasks));
    return merged;
  }

  boolean valid() {
    if (!attributable()) {
      return false;
    }

    int energyConsumed = 0;
    for (int socket = 0; socket < SOCKETS; socket++) {
      energyConsumed += energy.getEnergy(socket);
    }

    if (energyConsumed == 0) {
      return false;
    }

    int sysJiffies = 0;
    for (int socket = 0; socket < SOCKETS; socket++) {
      sysJiffies += cpu.getJiffies(socket);
    }

    if (sysJiffies == 0) {
      return false;
    }

    int appJiffies = 0;
    for (int socket = 0; socket < SOCKETS; socket++) {
      appJiffies += tasks.getJiffies(socket);
    }

    if (appJiffies > sysJiffies) {
      return false;
    }

    return true;
  }

  boolean attributable() {
    if (TimeUtil.equal(energy.getTimestamp(), Instant.MAX) || TimeUtil.equal(energy.getTimestamp(), Instant.MAX) || TimeUtil.equal(energy.getTimestamp(), Instant.MAX)) {
      return false;
    } else {
      return true;
    }
  }

  Instant getTimestamp() {
    return end;
  }
}
