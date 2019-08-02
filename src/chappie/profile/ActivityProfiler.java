package chappie.profile.impl;

public class ActivityProfiler implements Profiler {
  long start;
  double[] initialRaplReading;

  public ActivityProfiler(Config config) {
    this.config = config;
    this.start = System.currentTimeMillis();
  }

  private ArrayList<ArrayList<Object>> activity = new ArrayList<ArrayList<Object>>();

  public void sample(int epoch, long epochTime) {
    long startTime = System.nanoTime();

    ArrayList<Object> record = new ArrayList<Object>();

    record.add(epoch);
    record.add(epochTime);
    record.add((double)(totalTime) / 1000000);
    record.add((double)(readingTime) / totalTime);

    activeness.add(record);
  }

  private void dump() {
    CSVPrinter printer = new CSVPrinter(
      new FileWriter(config.workDirectory + "/chappie.activity" + config.suffix + ".csv"),
      CSVFormat.DEFAULT.withHeader("epoch", "timestamp", "elapsed", "activity")
    );

    printer.printRecords(activity);
    printer.close();
  }
}
