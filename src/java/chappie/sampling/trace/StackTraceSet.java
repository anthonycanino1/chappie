package chappie.sampling.trace;

import chappie.profiling.SampleSet;
import chappie.profiling.Sample;
import java.util.ArrayList;

/* Collection of stack trace samples. I don't remember why this is useful now. */
public final class StackTraceSet implements SampleSet<StackTraceSample> {
  private final ArrayList<StackTraceSample> samples = new ArrayList<>();

  StackTraceSet(ArrayList<String> records) {
    for (String record: records) {
      this.samples.add(new StackTraceSample(record));
    }
  }

  @Override
  public Iterable<StackTraceSample> getSamples() {
    return samples;
  }
}
