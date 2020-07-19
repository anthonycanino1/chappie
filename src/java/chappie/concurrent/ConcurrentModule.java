package chappie.concurrent;

import chappie.profiling.Sampler;
import dagger.Module;
import dagger.Provides;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Module to provide an executor for the profiler. Users can provide their
 * own executor if they like (unit testing).
 */
@Module
public interface ConcurrentModule {
  // make sure each sampler has a thread
  @Provides
  static ExecutorService provideExecutor(Set<Sampler> samplers) {
    final AtomicInteger counter = new AtomicInteger();
    return Executors.newFixedThreadPool(
      samplers.size(), r -> new Thread(r,
        String.join("-",
          "chappie",
          String.format("%02d", counter.getAndIncrement()))));
  }
}
