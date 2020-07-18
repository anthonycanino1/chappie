// package chappie.calmness;
//
// import chappie.util.profiling.Profile;
// import java.lang.Math;
// import java.util.Arrays;
// import java.util.List;
//
// /**
// * A CPU Frequency storage object that implements the raw data structure used in
// * our FSE2020 paper (fse2020/analysis/spatial-plot.py and fse2020/analysis/spatial-plot.py)
// * to compute runtime calmness. We need to determine how we want to use these online
// * outside of post-run evaluation. Implicitly, these histograms are sorted along
// * the temporal axis.
// */
// public final class CalmnessProfile implements Profile {
//   private final FrequencyHistogram[] freqs;
//
//   CalmnessProfile(FrequencyHistogram[] freqs) {
//     this.freqs = freqs;
//   }
//
//   CalmnessProfile(List<FrequencyHistogram> freqs) {
//     this.freqs = freqs.toArray(new FrequencyHistogram[0]);
//   }
//
//   /** Computes the adjacent correlation of all histograms. */
//   @Override
//   public double evaluate() {
//     if (freqs.length > 1) {
//       return correlate(new CalmnessProfile(Arrays.copyOfRange(freqs, 1, freqs.length - 1)));
//     } else {
//       return 0;
//     }
//   }
//
//   /** Can only compare with other calmness profiles. */
//   @Override
//   public double compare(Profile other) {
//     if (other instanceof CalmnessProfile) {
//       return correlate((CalmnessProfile)other);
//     } else {
//       return 0;
//     }
//   }
//
//   /** Can only compare with other calmness profiles. */
//   @Override
//   public String dump() {
//     return "";
//   }
//
//   @Override
//   public String toString() {
//     String message = "Freq profile:\n";
//     for (FrequencyHistogram freq: freqs) {
//       message += freq.bin();
//     }
//     return message;
//   }
//
//   /**
//   * Computes the correlation across the matching histograms. This logic isn't
//   * quite correct yet because of the need for temporal rebinning.
//   */
//   private double correlate(CalmnessProfile other) {
//     int n = Math.min(this.freqs.length, other.freqs.length);
//     double corr = 0;
//     for (int i = 0; i < n; i++) {
//       corr += this.freqs[i].correlate(other.freqs[i]);
//     }
//     return corr / n;
//   }
// }
