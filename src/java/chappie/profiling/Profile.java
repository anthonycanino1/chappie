package chappie.profiling;

/** Interface for an evaluatable object. */
public interface Profile {
  /** Returns the profile's score (autocorrelation for example). */
  double evaluate();

  /** Returns the value between this and another profile. There is not a good
  *    handling on this right now, so the implementer will have to type check to
  *    prevent errors.
  */
  double compare(Profile other);

  /** Returns the profile expressed as ????. */
  String dump();
}
