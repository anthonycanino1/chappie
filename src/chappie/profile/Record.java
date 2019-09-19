package chappie.profile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public abstract class Record {
  // Record structure to support lazy parsing (for records that are originally
  // strings) and some automated writing methods for dumping to CSV
  // The record implementation for each profiler defines the data fields
  protected int epoch;

  @Override
  public String toString() {
    return Integer.toString(epoch) + ";" + stringImpl();
  };
  protected abstract String stringImpl();

  public String[] getHeader() { return headerImpl(); }
  protected abstract String[] headerImpl();
}
