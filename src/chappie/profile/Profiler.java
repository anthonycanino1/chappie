package chappie.profile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public abstract class Profiler {
  protected int rate;
  protected Logger logger;
  public Profiler(int rate, int time) {
    this.rate = rate;
    this.logger = Logger.getLogger("chappie");
    logger.info(this.getClass().getSimpleName() + ": " + rate * time + " ms");
  }

  protected ArrayList<Record> data = new ArrayList<Record>();
  public void sample(int epoch) {
    if (epoch % rate == 0) {
      sampleImpl(epoch);
    }
  };

  protected abstract void sampleImpl(int epoch);

  public void dump() throws IOException {
    logger.info("writing " + this.getClass().getSimpleName() + " data");
    dumpImpl();
  }

  public abstract void dumpImpl() throws IOException;
}
