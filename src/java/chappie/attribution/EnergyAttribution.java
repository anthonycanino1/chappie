package chappie.attribution;

import chappie.profiling.Profile;
import java.time.Instant;

/**
* Representation of the application's power usage. Included is the total energy
* consumed, the energy attributed to the application, and the time range the
* attribution is associated with.
*
* NOTE: it is currently recommended to implement toString as a csv dump.
*
*/
public interface EnergyAttribution extends Profile {
  double getApplicationEnergy();

  double getTotalEnergy();

  Instant getStart();

  Instant getEnd();
}
