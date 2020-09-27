// package chappie.calmness;
//
// import chappie.util.concurrent.SelfScheduledRunnable;
// import chappie.util.concurrent.RunnableCollectionExecutor;
// import chappie.util.profiling.Profile;
// import chappie.util.profiling.Profiler;
// import java.util.ArrayList;
// import java.util.concurrent.atomic.AtomicInteger;
// import java.util.logging.Logger;
// import jlibc.proc.CPU;
//
// /**
// * Collects the cpu frequency data and stores it in adjustable histograms for
// * online evaluation. This implements the same logic used in our FSE2020
// * paper (fse2020/analysis/spatial-plot.py and fse2020/analysis/spatial-plot.py)
// * to compute runtime calmness. The cpu frequencies are sampled and stored in a
// * list of adjustable histograms.
// */
// public final class CalmnessProfiler implements Profiler {
//   private final Logger logger = Logger.getLogger("chappie");
//   private final RunnableCollectionExecutor executor = new RunnableCollectionExecutor();
//   private final ArrayList<FrequencyHistogram> data = new ArrayList<>();
//
//   /* Sets up the executor to run the cpu frequency collection. */
//   public CalmnessProfiler(int rate) {
//     ArrayList<SelfScheduledRunnable> runnables = new ArrayList<>();
//     runnables.add(new SelfScheduledRunnable(() ->
//         data.add(new FrequencyHistogram(CPU.getFreqs())), rate));
//
//     for (SelfScheduledRunnable runnable: runnables) {
//       executor.add(runnable);
//     }
//
//     final AtomicInteger counter = new AtomicInteger();
//     executor.setThreadFactory(r -> new Thread(
//       r,
//       String.join(
//         "-",
//         "chappie",
//         "calm",
//         String.format("%02d", counter.getAndIncrement()))));
//   }
//
//   @Override
//   public void start() {
//     logger.info("starting calmness profiling");
//     executor.start();
//   }
//
//   @Override
//   public void stop() {
//     logger.info("stopping calmness profiling");
//     executor.stop();
//   }
//
//   @Override
//   public Profile getProfile() {
//     return new CalmnessProfile(data);
//   }
// }
