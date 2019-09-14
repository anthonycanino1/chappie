package chappie.profile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

public abstract class Profiler {
  protected int rate;
  protected Logger logger;
  public Profiler(int rate, int time) {
    this.rate = rate;
    this.logger = Logger.getLogger("chappie");
    logger.info(this.getClass().getSimpleName() + ": " + rate * time + " ms");
  }

  public interface Record { }
  protected ArrayList<Record> data = new ArrayList<Record>();
  public void sample(int epoch) {
    if (epoch % rate == 0) {
      sampleImpl(epoch);
    }
  };
  protected abstract void sampleImpl(int epoch);

  public abstract void dump() throws IOException;
}
