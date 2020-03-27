package chappie.profile.processing;

import static jrapl.EnergyCheckUtils.socketNum;

import chappie.profile.processing.EnergyProfile;
import chappie.profile.processing.TraceProfile;
import chappie.profile.sampling.RAPLSampler.EnergyRecord;
import chappie.profile.sampling.TaskSampler.TaskRecordSet;
import chappie.profile.sampling.CPUSampler.CPURecord;
import chappie.profile.sampling.TraceSampler.TraceRecordSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.ArrayList;

public class Attributer {
  private final long start = System.currentTimeMillis();
  private final ArrayList<EnergyRecord> energy = new ArrayList<>();
  private final ArrayList<TaskRecordSet> task = new ArrayList<>();
  private final ArrayList<CPURecord> cpu = new ArrayList<>();
  private final ArrayList<TraceRecordSet> trace = new ArrayList<>();

  public void add(EnergyRecord record) {
    energy.add(record);
  }

  public void add(TaskRecordSet record) {
    task.add(record);
  }

  public void add(CPURecord record) {
    cpu.add(record);
  }

  public void add(TraceRecordSet record) {
    trace.add(record);
  }

  public ArrayList<TraceProfile> attribute() {
    HashMap<String, Double> energyCounter = new HashMap<>();
    double profile = 0;
    while (!this.energy.isEmpty() && !this.task.isEmpty() && !this.cpu.isEmpty() && !this.trace.isEmpty()) {
      long timestamp = System.currentTimeMillis();
      EnergyRecord energy = this.energy.get(0); this.energy.remove(0);
      TaskRecordSet task = this.task.get(0); this.task.remove(0);
      CPURecord cpu = this.cpu.get(0); this.cpu.remove(0);
      TraceRecordSet trace = this.trace.get(0); this.trace.remove(0);

      double attributed = 0;
      for (int i = 0; i < socketNum; i++) {
        if (cpu.getJiffies(i) > 0) {
          attributed += (double)(task.getJiffies(i)) / cpu.getJiffies(i) * energy.getReading(i).getEnergy();
        }
        // attributed += energy.getReading(i).getEnergy();
      }

      Set<Integer> alignableTasks = new HashSet<>(task.getTaskIds());
      alignableTasks.retainAll(trace.getTaskIds());

      double aligned = attributed / (double)task.getTaskIds().size();
      for (Integer id: alignableTasks) {
        String stackTrace = TraceProfile.getDeepTrace(trace.getTraceRecord(id).getStackTrace());
        energyCounter.putIfAbsent(stackTrace, 0.0);
        energyCounter.put(stackTrace, energyCounter.get(stackTrace) + aligned);
      }
    }

    ArrayList<TraceProfile> profiles = new ArrayList<>();
    for (Entry<String, Double> entry: energyCounter.entrySet()) {
      profiles.add(new TraceProfile(entry.getKey(), entry.getValue()));
    }

    return profiles;
  }

  @Override
  public String toString() {
    return energy.toString() + task.toString() + cpu.toString();
  }
}
