package chappie_util.wrapper;

import chappie.Chappie;
import chappie.util.profiling.Profile;
import org.dacapo.harness.Callback;
import org.dacapo.harness.CommandLineArgs;

public class DaCapo extends Callback {
  Profile lastProfile;

  public DaCapo(CommandLineArgs args) {
    super(args);
  }

  @Override
  public void start(String benchmark) {
    Chappie.start();
    super.start(benchmark);
  }

  @Override
  public void stop(long duration) {
    super.stop(duration);
    Profile profile = Chappie.stop();
    System.out.println(profile);
    // System.out.println("profile similarity: " + profile.compare(lastProfile));
  }
}
