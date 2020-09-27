// package chappie.calmness;
//
// import chappie.util.MathUtil;
// import chappie.util.profiling.Profile;
// import chappie.util.profiling.Sample;
// import chappie.util.profiling.SampleProcessor;
// import java.lang.Iterable;
// import java.lang.Math;
// import java.util.Arrays;
// import java.util.List;
//
// /** Adjustable histogram of frequency values. */
// public class FrequencyHistogram implements SampleProcessor {
//   // TODO(timur): redefine this; it sucks.
//   public enum Frequency {
//     GHZ(1000000), MHZ(1000), KHZ(1);
//
//     private final long value;
//     Frequency(long value) {
//       this.value = value;
//     }
//
//     public long value() {
//       return value;
//     }
//   }
//
//   public enum Resolution {
//     HUNDREDS(100), TENS(10), ONES(1);
//
//     private final long value;
//     Resolution(long value) {
//       this.value = value;
//     }
//
//     public long value() {
//       return value;
//     }
//   }
//
//   /** Combines all the data in a variable number of histograms. */
//   public static FrequencyHistogram merge(FrequencyHistogram... hists) {
//     FrequencyHistogram hist = new FrequencyHistogram(new long[0]);
//     for (FrequencyHistogram h: hists) {
//       hist = hist.add(h);
//     }
//
//     return hist;
//   }
//
//   /** Combines all the data in a iterable of histograms. */
//   public static FrequencyHistogram merge(Iterable<FrequencyHistogram> hists) {
//     FrequencyHistogram hist = new FrequencyHistogram(new long[0]);
//     for (FrequencyHistogram h: hists) {
//       hist = hist.add(h);
//     }
//
//     return hist;
//   }
//
//   private final long[] data;
//   private final int uniqueCount;
//
//   private Frequency frequency = Frequency.MHZ;
//   private Resolution resolution = Resolution.TENS;
//
//   private long[] bins;
//   private int[] counts;
//
//   public FrequencyHistogram(long[] data) {
//     this.data = data.clone();
//     Arrays.sort(this.data);
//
//     uniqueCount = (int) Arrays.stream(data).distinct().count();
//   }
//
//   public void setFrequency(Frequency frequency) {
//     this.frequency = frequency;
//   }
//
//   public Frequency getFrequency() {
//     return frequency;
//   }
//
//   public void setResolution(Resolution resolution) {
//     this.resolution = resolution;
//   }
//
//   public Resolution getResolution() {
//     return resolution;
//   }
//
//   private FrequencyHistogram bin(long start, long end, int binCount) {
//     if (binCount <= uniqueCount) {
//       binCount = uniqueCount;
//     }
//     bins = new long[binCount];
//     for (int i = 0; i < binCount; i++) {
//       bins[i] = start + i * (end - start + 1) / binCount;
//     }
//
//     long res = frequency.value() * resolution.value();
//     counts = new int[binCount];
//     for (long value: data) {
//       int current = binCount / 2;
//       int lower = 0;
//       int upper = binCount;
//       while (true) {
//         if (value / res > bins[current] && current > lower) {
//           lower = current;
//           current = (upper + current) / 2;
//         } else if (value / res < bins[current] && current < upper) {
//           upper = current;
//           current = (lower + current) / 2;
//         } else {
//           counts[current]++;
//           break;
//         }
//       }
//     }
//
//     return this;
//   }
//
//   public FrequencyHistogram bin(int binCount) {
//     long res = frequency.value() * resolution.value();
//     return bin(data[0] / res, data[data.length - 1] / res, binCount);
//   }
//
//   public FrequencyHistogram bin() {
//     return bin(FDBinCount());
//   }
//
//   public FrequencyHistogram bin(FrequencyHistogram other) {
//     return bin(other.data[0], other.data[other.data.length - 1], other.bins.length);
//   }
//
//   public FrequencyHistogram add(FrequencyHistogram other) {
//     int size = this.data.length + other.data.length;
//     long[] mergedData = Arrays.copyOf(this.data, size);
//     System.arraycopy(other.data, 0, mergedData, this.data.length, other.data.length);
//     return new FrequencyHistogram(mergedData);
//   }
//
//   public double correlate(FrequencyHistogram other) {
//     FrequencyHistogram merged = this.add(other).bin();
//     this.bin(merged);
//     other.bin(merged);
//
//     return MathUtil.pcc(this.counts, other.counts);
//   }
//
//   @Override
//   public String toString() {
//     String message = "";
//     if (bins == null) { bin(); }
//     for (int i = 0; i < bins.length; i++) {
//       message += bins[i] + " -> " + counts[i] + "\n";
//     }
//     if (message.length() > 0) {
//       return message.substring(0, message.length() - 1);
//     } else {
//       return message;
//     }
//   }
//
//   private int FDBinCount() {
//     double iqr = data[(int)(0.75 * data.length)] - data[(int)(0.25 * data.length)];
//     if (iqr > 0) {
//       double range = data[data.length - 1] - data[0];
//       return (int)(Math.cbrt(data.length) * range / iqr / 2);
//     } else {
//       return (int)Math.sqrt(data.length);
//     }
//   }
// }
