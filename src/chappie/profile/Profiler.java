package chappie.profile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;

public abstract class Profiler {
  protected int rate;
  protected Logger logger;
  protected String workDirectory;

  public Profiler(int rate, int time, String workDirectory) {
    this.rate = rate;
    this.logger = Logger.getLogger("chappie");
    this.workDirectory = workDirectory;
    logger.info(this.getClass().getSimpleName() + " set to " + rate * time + " ms");
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
