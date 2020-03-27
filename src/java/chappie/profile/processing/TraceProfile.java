package chappie.profile.processing;

import java.util.Queue;
import java.util.ArrayList;

public class TraceProfile {
  private final String trace;
  private final double energy;

  public static String getDeepTrace(String trace) {
    for (String method: trace.split("@")) {
      boolean skip = !method.contains(".") ||
        method.contains("java.") ||
        method.contains("javax.") ||
        method.contains("jdk.") ||
        method.contains("sun.") ||
        method.contains("org.apache.commons.") ||
        method.contains("<init>") ||
        method.contains(".so") ||
        method.contains("::") ||
        method.contains("[") ||
        method.contains("]");

      if (!skip) {
        return method;
      }
    }
    return "";
  }

  public TraceProfile(String trace, double energy) {
    this.trace = trace;
    this.energy = energy;
  }

  public String getTrace() {
    return trace;
  }

  public double getEnergy() {
    return energy;
  }

  @Override
  public String toString() {
    return trace + "@" + String.format("%.02f", energy);
  }
}
