// package chappie.attribution;
//
// import chappie.util.profiling.Profile;
// import java.lang.Math;
// import java.util.Arrays;
// import java.util.List;
//
// /**
// * Representation of the application's power usage. Included is the total energy
// * consumed, the energy attributed to the application, and the relative
// * energy consumption of sampled methods in some interval.
// */
// public final class AttributionProfile implements Profile {
//   // static AttributionProfile merge(AttributionProfile... profiles) {
//   //   return merge(Arrays.asList(profiles));
//   // }
//   //
//   // static AttributionProfile merge(Iterable<AttributionProfile> profiles) {
//   //   EnergyAttribution mergedAttribution = new EnergyAttribution();
//   //   StackTraceRanking mergedRanking = new StackTraceRanking();
//   //   for (AttributionProfile profile: profiles) {
//   //     mergedAttribution = EnergyAttribution.merge(mergedAttribution, profile.attribution);
//   //     mergedRanking = StackTraceRanking.merge(mergedRanking, profile.ranking);
//   //   }
//   //   return new AttributionProfile(mergedAttribution, mergedRanking);
//   // }
//
//   private final long timestamp = System.currentTimeMillis();
//   private final EnergyAttribution attribution;
//   private final StackTraceRanking ranking;
//
//   /** Returns the percent of total machine power consumed. */
//   @Override
//   public double evaluate() {
//     return 0;
//     // double attributed = 0;
//     // double total = 0;
//     // for (int i = 0; i < this.total.length; i++) {
//     //   attributed += this.attributed[i].getEnergy();
//     //   total += this.total[i].getEnergy();
//     // }
//     // if (total == 0) {
//     //   return 0;
//     // } else {
//     //   return attributed / total;
//     // }
//   }
//
//   /** Can only compare with other attribution profiles. */
//   @Override
//   public double compare(Profile other) {
//     return 0;
//     // if (other instanceof AttributionProfile) {
//     //   return correlate((AttributionProfile)other);
//     // } else {
//     //   return 0;
//     // }
//   }
//
//   @Override
//   public String dump() {
//     return "";
//   }
//
//   @Override
//   public String toString() {
//     return String.join(System.lineSeparator(),
//       "energy attribution:",
//       attribution.toString(),
//       "stack trace ranking:",
//       ranking.toString());
//   }
//
//   public EnergyAttribution getAttribution() {
//     return attribution;
//   }
//
//   public StackTraceRanking getRanking() {
//     return ranking;
//   }
//
//   AttributionProfile(EnergyAttribution attribution, StackTraceRanking ranking) {
//     this.attribution = attribution;
//     this.ranking = ranking;
//   }
// }
