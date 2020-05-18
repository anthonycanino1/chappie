package chappie.util.profiling;

/** Simple interface for a processor. */
public interface SampleProcessor <P extends Profile> {
  void add(Sample s);
  P process();
}
