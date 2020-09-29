package chappie;

import clerk.DataSource;
import clerk.Processor;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;
import eflect.data.CpuSample;
import eflect.data.RaplSample;
import eflect.data.TaskSample;
import one.profiler.AsyncProfiler;
import one.profiler.Events;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/** Module to provide the eflect implementation. */
@Module
interface StackTraceRankingModule {
  // fix this; probably should be module-based as well
  static final int DEFAULT_RATE_MS = 1;
  static final Duration asyncRate = Duration.ofMillis(Long.parseLong(System.getProperty(
      "chappie.async.rate",
      Integer.toString(DEFAULT_RATE_MS))));
  static final boolean noAsync = !setupAsync();

  // adapted from https://github.com/adamheinrich/native-utils
  static File createLibraryFileFromJar(String library) throws IOException {
    File temp = File.createTempFile(library, null);
    temp.deleteOnExit();

    if (!temp.exists()) {
      throw new FileNotFoundException("Could not create a temporary file.");
    }

    // Prepare buffer for data copying
    byte[] buffer = new byte[1024];
    int readBytes;

    try (InputStream is = StackTraceRankingModule.class.getResourceAsStream(library)) {
      if (is == null) {
        throw new FileNotFoundException("Could not find library " + library + " in jar.");
      }
      // Open output stream and copy data between source file in JAR and the temporary file
      try (FileOutputStream os = new FileOutputStream(temp)) {
        try {
          while ((readBytes = is.read(buffer)) != -1) {
            os.write(buffer, 0, readBytes);
          }
        } finally {
          return temp;
        }
      }
    }
  }

  /** Set up and run the async-profiler. */
  static boolean setupAsync() {
    // Logger logger = LoggerUtil.setup();
    try {
      long rate = asyncRate.getNano(); // only supporting sub-second for the moment
      AsyncProfiler.getInstance(createLibraryFileFromJar("/external/asyncProfiler/libasyncProfiler.so").getPath()).start(Events.CPU, asyncRate.getNano());
      // logger.info("started async-profiler at " + asyncRate);
      return true;
    } catch (Exception e) {
      // System.out.println(e);
      e.printStackTrace();
      // logger.log(WARNING, "unable to start async-profiler", e);
      return false;
    }
  }

  /** Returns un-split string of async-profiler records while safely pausing the profiler. */
  static String sampleStackTraces() {
    if (!noAsync) {
      AsyncProfiler.getInstance().stop();
      String traces = AsyncProfiler.getInstance().dumpRecords();
      AsyncProfiler.getInstance().resume(Events.CPU, asyncRate.getNano());
      return traces;
    }
    return "";
  }

  @Provides
  @DataSource
  static Set<Supplier<?>> provideSources() {
    return Set.of(StackTraceRankingModule::sampleStackTraces, TaskSample::new, CpuSample::new, RaplSample::new);
  }

  @Provides
  static Processor<?, Map<String, Double>> provideProcessor() {
    return new StackTraceRanker();
  }
}
