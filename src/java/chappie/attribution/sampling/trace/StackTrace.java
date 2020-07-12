package chappie.attribution.sampling.trace;

import java.util.function.Predicate;

/** Representation of a stack trace with simple search operations. */
// TODO(timur): this is a mess; it's totally unusable outside of me
public final class StackTrace {
  /** Checks if the method name is a native method. */
  public static boolean isNativeMethod(String method) {
    return !method.contains(".")
      || method.contains("java.")
      || method.contains("javax.")
      || method.contains("jdk.")
      || method.contains("sun.")
      || method.contains("org.apache.commons.")
      || method.contains("<init>")
      || method.contains(".so")
      || method.contains("::")
      || method.contains("[")
      || method.contains("]");
  }

  /** Checks if the method name is from a chappie library. */
  public static boolean isChappieMethod(String method) {
    return method.contains("chappie")
      || method.contains("jlibc")
      || method.contains("stoke")
      || method.contains("jrapl")
      || method.contains("AsyncProfiler");
  }

  /** Checks if the method name genuinely comes from the application. */
  public static boolean isApplicationMethod(String method) {
    return !isNativeMethod(method) && !isChappieMethod(method);
  }

  public final int length;

  private final String stackTrace;

  StackTrace(String stackTrace) {
    this.stackTrace = stackTrace;
    if (stackTrace == "") {
      length = 0;
    } else {
      length = stackTrace.split(";").length;
    }
  }

  /** Returns the top method of the stack trace. */
  public StackTrace getCallingMethod() {
    return getCallingMethod(0);
  }

  /** Returns the n-th method from top of the stack trace. */
  public StackTrace getCallingMethod(int depth) {
    return new StackTrace(stackTrace.split(";")[depth]);
  }

  /**
  * Returns a stack trace where the all methods that fail to meet some condition
  * are removed until it is satisfied.
  */
  // TODO(timur): handle empty
  public StackTrace stripUntil(Predicate<String> stopCondition) {
    String strippedTrace = "";
    boolean stop = false;
    for (String method: stackTrace.split(";")) {
      stop |= stopCondition.test(method);

      if (stop)
        strippedTrace += method + ";";
    }
    return new StackTrace(strippedTrace);
  }

  @Override
  public String toString() {
    return stackTrace;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof StackTrace) {
      return this.stackTrace.equals(((StackTrace) o).stackTrace);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return stackTrace.hashCode();
  }
}
