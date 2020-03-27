package chappie.profile;

import chappie.util.LoggerUtil;
import chappie.profile.Record;
import chappie.profile.sampling.ThreadSampler;
import chappie.util.ThreadUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

public final class Profiler implements Runnable {
  public static Profiler buildProfiler(int samplingRate, Runnable sampler) {
    return new Profiler(samplingRate, sampler);
  }

  private final int samplingRate;
  private final Runnable sampler;

  private Profiler(int samplingRate, Runnable sampler) {
    this.samplingRate = samplingRate;
    this.sampler = sampler;
  }

  @Override
  public void run()  {
    boolean done = false;
    Logger logger = LoggerUtil.getLogger();

    while (!done) {
      // track the time for each stage
      long start = System.nanoTime();
      sampler.run();
      // try {
      //   logger.info(record.toString());
      // } catch (Exception e) {
      //
      // }

      long elapsed = System.nanoTime() - start;
      try {
        ThreadUtil.sleepUntil(start, samplingRate);
      } catch (InterruptedException e) {
        done = true;
      }

      long total = System.nanoTime() - start;
      Record activity = new ProfilerRecord(elapsed, total);
    }

    // logger.info(data.size() + " records collected");
    // for (RecordSet recordSet: data) {
    //   logger.info("" + recordSet.getTimestamp());
    //   for (Record record: recordSet.getRecords())
    //     logger.info(record.toString());
    // }

    // // temporary variable
    // private static int sessionId = 0;
    // private void dump() throws IOException {
    // // String workDirectoryRoot = workDirectory;
    // // workDirectory = workDirectoryRoot + '/' + sessionId;
    // // new File(workDirectory).mkdir();
    //
    // logger.info("writing chappie data to " + workDirectory);
    //
    // CSV.write(data, ChappieRecord.getHeader(), Chaperone.getWorkDirectory() + "/chappie.csv");
    // JSON.write(timestamps, Chaperone.getWorkDirectory() + "/time.json");
    //
    // for (Profiler profiler: profilers)
    //   profiler.dump();
    //
    // logger.info("done writing data");

    // workDirectory = workDirectoryRoot;
  }

  private static class ProfilerRecord implements Record {
    private final long elapsed;
    private final long total;

    public ProfilerRecord(long elapsed, long total) {
      this.elapsed = elapsed;
      this.total = total;
    }

    @Override
    public String toString() {
      return elapsed + "," + total;
    }
  }

  private static class RecordSet {
    private final long timestamp;
    private final Collection<Record> records;

    public RecordSet(long timestamp, Collection<Record> records) {
      this.timestamp = timestamp;
      this.records = records;
    }

    public long getTimestamp() {
      return timestamp;
    }

    public Collection<Record> getRecords() {
      return records;
    }
  }
}
