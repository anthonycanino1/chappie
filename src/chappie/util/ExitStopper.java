package chappie.util;

import java.security.Permission;
import java.lang.SecurityManager;

public class ExitStopper extends SecurityManager {
  @Override public void checkExit(int status) { throw new SecurityException(); }

  @Override public void checkPermission(Permission perm) { }
}
