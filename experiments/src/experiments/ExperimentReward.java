package experiments;

import experiments.ExperimentProfiler;
import experiments.processing.ExperimentEnergyAttribution;
import com.stoke.Reward;
import java.util.ArrayList;
import java.util.logging.Logger;

public final class ExperimentReward extends Reward {
  private static final Logger logger = Logger.getLogger("chappie");

  private final ExperimentProfiler profiler;

  private int lastProfileIndex = 0;

  public ExperimentReward(ExperimentProfiler profiler) {
    this.profiler = profiler;
  }

  @Override
  public double valuate() {
    return getEnergy();
  }

  @Override
  public double SLA() {
    return 50.0;
  }

  public boolean check() {
    return ((ArrayList<ExperimentEnergyAttribution>) profiler.getProfiles()).size() > lastProfileIndex;
  }

  private double getEnergy() {
    double energy = 0;
    try {
      ArrayList<ExperimentEnergyAttribution> profiles = (ArrayList<ExperimentEnergyAttribution>) profiler.getProfiles();
      for (ExperimentEnergyAttribution profile: profiles.subList(lastProfileIndex, profiles.size() - 1)) {
        energy += profile.getApplicationEnergy();
      }
      lastProfileIndex = profiles.size();
    } catch (Exception e) {
      logger.info("got an exception:");
      e.printStackTrace();
    }

    return energy;
  }
}
