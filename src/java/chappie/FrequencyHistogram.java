package chappie;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.Arrays;

/** Adjustable histogram of frequency values. */
public class FrequencyHistogram {
  private static int CPU_COUNT = Runtime.getRuntime().availableProcessors();
  private static final String FREQS_PREFIX = "/sys/devices/system/cpu";
  private static final String FREQS_SUFFIX = "cpufreq/cpuinfo_cur_freq";

  private static long[] getFreqs() {
    long[] freqs = new long[CPU_COUNT];
    for (int i = 0; i < CPU_COUNT; i++) {
      try {
        String freqFile = String.join(File.separator,
          FREQS_PREFIX,
          "cpu" + Integer.toString(i),
          FREQS_SUFFIX);
        freqs[i] = Long.parseLong(Files.readString(Paths.get(freqFile)).split("\n")[0]);
      } catch (IOException e) {
        e.printStackTrace();
        freqs[i] = -1;
      }
    }
    return freqs;
  }

  private final long[] data;

  FrequencyHistogram() {
    this.data = getFreqs();
  }

  @Override
  public String toString() {
    return Arrays.toString(data);
  }
}
