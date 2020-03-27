package chappie.profile.processing;

public class EnergyProfile {
  private final long timestamp;
  private final double energy;

  public EnergyProfile(long timestamp, double energy) {
    this.timestamp = timestamp;
    this.energy = energy;
  }

  public double getEnergy() {
    return energy;
  }

  @Override
  public String toString() {
    return String.format("%.02f", energy) + "@" + timestamp;
  }
}
