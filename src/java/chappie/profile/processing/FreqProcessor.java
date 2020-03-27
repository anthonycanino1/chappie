package chappie.profile.processing;

public class FreqProcessor {
  public static long[] sample(long[] freqs) {
    for (int i = 0; i < freqs.length; i++)
      freqs[i] /= 10000;
    
    return freqs;
  }
}
