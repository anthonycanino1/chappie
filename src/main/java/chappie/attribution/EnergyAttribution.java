package chappie.attribution;

import chappie.profiling.Profile;
import java.time.Instant;

/**
* Representation of the application's power usage. Included is the total energy
* consumed, the energy attributed to the application, and the amount of energy
* each task in the attribution should be assigned.
*/
public interface EnergyAttribution extends Profile {
  double getApplicationEnergy();

  double getTotalEnergy();

  Instant getStart();

  Instant getEnd();
}
