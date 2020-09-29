package chappie.experiments;

import chappie.ChappieProfiler;
import eflect.EnergyFootprint;
import org.dacapo.harness.Callback;
import org.dacapo.harness.CommandLineArgs;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Map.Entry;

public final class DaCapo extends Callback {
  private int iteration = 0;
  private ChappieProfiler profiler;
  private final String outputPath;

  public DaCapo(CommandLineArgs args) {
    super(args);
    outputPath = System.getProperty("chappie.output", "rankings");
    new File(outputPath).mkdir();
  }

  @Override
  public void start(String benchmark) {
    profiler = new ChappieProfiler();
    profiler.start();
    super.start(benchmark);
  }

  @Override
  public void stop(long duration) {
    super.stop(duration);
    Map<String, Double> rankings = profiler.stop();

    // write as a csv
    try (PrintWriter writer = new PrintWriter(new FileWriter(new File(outputPath, iteration + ".csv")))) {
      writer.println("trace,value");
      for (String method: rankings.keySet()) {
        writer.println(method + "," + rankings.get(method));
      }
    } catch (IOException e) {
      System.out.println("couldn't write chappie log");
      e.printStackTrace();
    }
    iteration++;
  }
}
