package chappie.attribution.processing;

import static jrapl.util.EnergyCheckUtils.SOCKETS;

import chappie.attribution.sampling.cpu.CPUSample;
import chappie.attribution.sampling.energy.EnergySample;
import chappie.attribution.sampling.tasks.TasksSample;
import chappie.util.profiling.Sample;
import chappie.util.profiling.SampleProcessor;
import java.time.Instant;

final class EnergyAttributer implements SampleProcessor<EnergyAttribution> {
  private Instant start;
  private Instant end;
  private TasksSample tasks;
  private CPUSample cpu;
  private EnergySample energy;

  @Override
  public void add(Sample s) {
    if (s == null) {
      return;
    }

    if (s instanceof EnergySample) {
      this.energy = (EnergySample) safeSampleMerge(this.energy, s);
    } else if (s instanceof TasksSample) {
      this.tasks = (TasksSample) safeSampleMerge(this.tasks, s);
    } else if (s instanceof CPUSample) {
      this.cpu = (CPUSample) safeSampleMerge(this.cpu, s);
    }

    if (start == null || s.getTimestamp().compareTo(start) < 0) {
      start = s.getTimestamp();
    }

    if (end == null || s.getTimestamp().compareTo(end) > 0) {
      end = s.getTimestamp();
    }
  }

  @Override
  public EnergyAttribution process() {
    return new EnergyAttribution(start, end, energy, cpu, tasks);
  }

  @Override
  public String toString() {
    return String.join("\n",
      start.toString() + "->" + end.toString(),
      energy != null ? energy.toString() : "",
      cpu != null ? cpu.toString() : "",
      tasks != null ? tasks.toString() : "");
  }

  EnergyAttributer merge(EnergyAttributer other) {
    EnergyAttributer merged = new EnergyAttributer();
    merged.add(safeSampleMerge(this.energy, other.energy));
    merged.add(safeSampleMerge(this.cpu, other.cpu));
    merged.add(safeSampleMerge(this.tasks, other.tasks));

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
    if (energy == null || cpu == null || tasks == null) {
      return false;
    } else {
      return true;
    }
  }

  Instant getTimestamp() {
    return end;
  }

  private Sample safeSampleMerge(Sample s1, Sample s2) {
    if (s1 != null && s2 != null) {
      return s1.merge(s2);
    } else if (s1 != null) {
      return s1;
    } else if (s2 != null) {
      return s2;
    } else {
      return null;
    }
  }
}
