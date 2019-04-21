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

  boolean no_rapl=false;
  boolean gem5_cmdline_dumpstats = false;
  int early_exit = -1;
  int sockets_no = -1;

  int iter = 0;

  public ChappieCallback(CommandLineArgs args) {
    super(args);

    // System.out.println("Starting Chappie ... Stay tuned!");
    sockets_no = -1;
    try {
    	sockets_no = Integer.parseInt(System.getenv("SOCKETS_NO"));
    } catch(Exception exc) {};

    no_rapl=false;
    	try {
    no_rapl = Boolean.parseBoolean(System.getenv("NO_RAPL"));
    	} catch(Exception exc) { }

    gem5_cmdline_dumpstats=false;
    try {
		    gem5_cmdline_dumpstats = Boolean.parseBoolean(System.getenv("GEM5_CMDLINE_DUMPSTATS"));
    } catch(Exception exc) { }

    early_exit = -1;
    try {
    	early_exit = Integer.parseInt(System.getenv("EARLY_EXIT"));
    } catch(Exception e) { }

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

    System.out.println("Chaperone Parameters:" +
                        "\n - Mode:\t\t\t" + mode +
                        "\n - VM Polling Rate:\t\t" + vmPolling + " milliseconds" +
                        "\n - OS Polling Rate:\t\t" + vmPolling * osPolling + " milliseconds" +
                        "\n - HP Polling Rate:\t\t" + hpPolling + " milliseconds" +
            						"\n - No Rapl:\t\t" + no_rapl + " ." +
            						"\n - Dump Gem5 Stats:\t\t" + gem5_cmdline_dumpstats + " ." +
            						"\n - Early Exit:\t\t" + early_exit + " ." +
            						"\n - Number of Sockets:\t\t" + sockets_no
                      );
  }

  @Override
  public void start(String benchmark) {
    setEnv("CHAPPIE_SUFFIX", Integer.toString(iter++));
    chaperone = new Chaperone(mode, vmPolling, osPolling,no_rapl,gem5_cmdline_dumpstats,early_exit,sockets_no);
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
