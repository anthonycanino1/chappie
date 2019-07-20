package chappie_util.dacapo;

import chappie.input.Config;
import chappie.SleepingChaperone;

import org.dacapo.harness.Callback;
import org.dacapo.harness.CommandLineArgs;

public class ChappieCallback extends Callback {
  Config config;
  SleepingChaperone chaperone;

  public ChappieCallback(CommandLineArgs args) {
    super(args);

    String configPath = System.getProperty("chappie.config", null);
    String workDir = System.getProperty("chappie.workDir", null);
    if (configPath == null) {
      System.out.println("no config found");
      System.exit(0);
    }

    config = Config.readConfig(configPath, workDir);
    System.out.println(config.toString());
  }

  int iter = 0;
  @Override
  public void start(String benchmark) {
    System.setProperty("chappie.suffix", Integer.toString(iter++));
    chaperone = new SleepingChaperone(config);
    super.start(benchmark);
  }

  @Override
  public void complete(String benchmark, boolean valid) {
    super.complete(benchmark, valid);
    chaperone.cancel();
  }
}
