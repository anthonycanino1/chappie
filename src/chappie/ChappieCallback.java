package chappie;

import chappie.Chaperone.ChappieMode;

import java.util.Map;
import java.lang.reflect.Field;

import org.dacapo.harness.Callback;
import org.dacapo.harness.CommandLineArgs;

public class ChappieCallback extends Callback {
  ChappieMode mode = ChappieMode.FULL;
  int polling = 4;

  int iter = 0;
  Chaperone chaperone;

  public ChappieCallback(CommandLineArgs args) {
    super(args);
    try {
      mode = ChappieMode.valueOf(System.getenv("MODE"));
    } catch(Exception e) { }

    try {
      polling = Integer.parseInt(System.getenv("POLLING_RATE"));
    } catch(Exception e) { }
  }

  @Override
  public void start(String benchmark) {
    setEnv("CHAPPIE_SUFFIX", Integer.toString(iter++));
    chaperone = new Chaperone(mode, polling);
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
