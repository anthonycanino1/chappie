package chappie;

import chappie.input.Config;
import chappie.Chaperone;

import java.util.Map;
import java.lang.reflect.Field;

import org.dacapo.harness.Callback;
import org.dacapo.harness.CommandLineArgs;

import java.io.File;

public class ChappieCallback extends Callback {
  Config config;
  Chaperone chaperone;

  public ChappieCallback(CommandLineArgs args) {
    super(args);

    String configPath = System.getProperty("chappie.config", null);
    if (configPath == null) {
      System.out.println("no config found");
      System.exit(0);
    }

    config = Config.readConfig(configPath);
    System.out.println(config.toString());
  }

  int iter = 0;
  @Override
  public void start(String benchmark) {
    System.setProperty("chappie.suffix", Integer.toString(iter++));
    chaperone = new Chaperone(config);
    super.start(benchmark);
  }

  @Override
  public void complete(String benchmark, boolean valid) {
    super.complete(benchmark, valid);
    chaperone.cancel();
  }
}
