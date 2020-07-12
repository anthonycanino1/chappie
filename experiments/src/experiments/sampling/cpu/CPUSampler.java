package experiments.sampling.cpu;

import chappie.profiling.Sampler;
import jlibc.proc.CPU;

/** Relative sampler for cpu jiffies so individual users can track usage. */
public final class CPUSampler implements Sampler<CPUSample> {
  private CPU[] last;

  public CPUSampler() { last = CPU.getCPUs(); }

  /** Returns the cpu sample of jiffies used since the last sample. */
  @Override
  public CPUSample sample() {
    CPU[] current = CPU.getCPUs();
    CPUSample record = new CPUSample(last, current);
    last = current;
    return record;
  }
}
