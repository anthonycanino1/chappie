package chappie;

import chappie.Chaperone.ChappieMode;

import java.util.Map;
import java.lang.reflect.Field;

import org.dacapo.harness.Callback;
import org.dacapo.harness.CommandLineArgs;

public class ChappieCallback extends Callback {
  Chaperone chaperone;

  ChappieMode mode = ChappieMode.FULL;

  int vmPolling = 4;
  int osPolling = 4;
  int hpPolling = 4;

  int iter = 0;

  public ChappieCallback(CommandLineArgs args) {
    super(args);
    try {
      mode = ChappieMode.valueOf(System.getenv("MODE"));
    } catch(Exception e) { }

    try {
      vmPolling = Integer.parseInt(System.getenv("VM_POLLING"));
    } catch(Exception e) { }

    try {
      osPolling = Integer.parseInt(System.getenv("OS_POLLING"));
    } catch(Exception e) { }

    try {
      hpPolling = Integer.parseInt(System.getenv("HP_POLLING"));
    } catch(Exception e) { }

    System.out.println("chappie Parameters:" +
                        "\n - Mode:\t\t\t" + mode +
                        "\n - VM Polling Rate:\t\t" + vmPolling + " milliseconds" +
                        "\n - OS Polling Rate:\t\t" + osPolling + " milliseconds" +
                        "\n - HP Polling Rate:\t\t" + hpPolling + " milliseconds"
                      );
  }

  @Override
  public void start(String benchmark) {
    setEnv("CHAPPIE_SUFFIX", Integer.toString(iter++));
    chaperone = new Chaperone(mode, vmPolling, osPolling);
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
