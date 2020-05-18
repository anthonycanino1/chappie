package chappie.util.profiling;

/** Simple interface for an evaluatable object. */
public interface Profile {
  double evaluate();
  double compare(Profile other);
  String dump();
}
