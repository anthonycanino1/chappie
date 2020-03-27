package chappie_util.wrapper;

import chappie.Chaperone;

import org.dacapo.harness.Callback;
import org.dacapo.harness.CommandLineArgs;

public class DaCapo extends Callback {
  public DaCapo(CommandLineArgs args) { super(args); }

  @Override
  public void start(String benchmark) {
    Chaperone.start();
    super.start(benchmark);
  }

  @Override
  public void stop(long duration) {
    super.stop(duration);
    Chaperone.stop();
  }
}
