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

    String configPath = System.getenv("CHAPPIE_CONFIG");
    config = Config.readConfig(configPath);
    System.out.println(config.toString());
  }

  int iter = 0;
  @Override
  public void start(String benchmark) {
    setEnv("CHAPPIE_SUFFIX", Integer.toString(iter++));
    chaperone = new Chaperone(config);
    super.start(benchmark);
  }

  @Override
  public void complete(String benchmark, boolean valid) {
    super.complete(benchmark, valid);
    chaperone.cancel();
  };

  public static void setEnv(String key, String value) {
    try {
        Map<String, String> env = System.getenv();
        Class<?> cl = env.getClass();
        Field field = cl.getDeclaredField("m");
        field.setAccessible(true);
        Map<String, String> writableEnv = (Map<String, String>) field.get(env);
        writableEnv.put(key, value);
    } catch (Exception e) {
        throw new IllegalStateException("Failed to set environment variable", e);
    }
  }
}
