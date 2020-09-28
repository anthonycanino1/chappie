package chappie;

import clerk.DataSource;
import clerk.Processor;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.function.Supplier;
/**
 * Module to provide calmness data, which is the current frequency values of the current linux system.
 *
 * <p> This currently only works for linux systems that provide the paths below. We need to look into
 *     extending this implementation across linux implementations.
 */
@Module
abstract class CalmnessModule {
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
        // let's report something real here? we may need to think about the logger again.
        // e.printStackTrace();
        freqs[i] = -1;
      }
    }
    return freqs;
  }

  @Provides
  @DataSource
  @IntoSet
  static Supplier<?> provideCalmnessSource() {
    return CalmnessModule::getFreqs;
  }

  // list storage
  @Provides
  static Processor<?, Iterable<long[]>> provideProcessor() {
    return new Processor<long[], Iterable<long[]>>() {
      private ArrayList<long[]> data = new ArrayList<>();

      @Override
      public void accept(long[] hist) {
        data.add(hist);
      }

      @Override
      public Iterable<long[]> get() {
        ArrayList<long[]> data = this.data;
        this.data = new ArrayList<>();
        return data;
      }
    };
  }
}
