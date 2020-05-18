package chappie.util;

import java.lang.Math;
import java.util.Map;
import java.util.Set;

/** Math functions that got reused enough times to belong here. */
public class MathUtil {
  /**
  * Computes the pearson correlation coefficient for two linear, ordered data
  * sets. If the data sets are not equally sized, we use the smaller dimension.
  */
  public static double pcc(int[] xs, int[] ys) {
    double sx = 0.0;
    double sy = 0.0;
    double sxx = 0.0;
    double syy = 0.0;
    double sxy = 0.0;

    int n = Math.min(xs.length, ys.length);

    for(int i = 0; i < n; ++i) {
      double x = xs[i];
      double y = ys[i];

      sx += x;
      sy += y;
      sxx += x * x;
      syy += y * y;
      sxy += x * y;
    }

    // covariation
    double cov = sxy / n - sx * sy / n / n;
    // standard error of x
    double sigmax = Math.sqrt(sxx / n -  sx * sx / n / n);
    // standard error of y
    double sigmay = Math.sqrt(syy / n -  sy * sy / n / n);

    // correlation is just a normalized covariation
    return cov / sigmax / sigmay;
  }

  public static double pcc(double[] xs, double[] ys) {
    double sx = 0.0;
    double sy = 0.0;
    double sxx = 0.0;
    double syy = 0.0;
    double sxy = 0.0;

    int n = Math.min(xs.length, ys.length);

    for(int i = 0; i < n; ++i) {
      double x = xs[i];
      double y = ys[i];

      sx += x;
      sy += y;
      sxx += x * x;
      syy += y * y;
      sxy += x * y;
    }

    // covariation
    double cov = sxy / n - sx * sy / n / n;
    // standard error of x
    double sigmax = Math.sqrt(sxx / n -  sx * sx / n / n);
    // standard error of y
    double sigmay = Math.sqrt(syy / n -  sy * sy / n / n);

    // correlation is just a normalized covariation
    return cov / sigmax / sigmay;
  }

  /**
  * Computes the pearson correlation coefficient for two maps.
  */
  public static <T> double pcc(Map<T, Double> X, Map<T, Double> Y) {
    Set<T> shared = X.keySet();
    shared.retainAll(Y.keySet());

    double[] xs = new double[shared.size()];
    double[] ys = new double[shared.size()];

    int i = 0;
    for (Object o: shared) {
      xs[i] = X.get(o);
      ys[i++] = Y.get(o);
    }

    if (xs.length > 0) {
      return pcc(xs, ys);
    } else {
      return 0;
    }
  }
}
