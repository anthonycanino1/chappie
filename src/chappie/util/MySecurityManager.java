package chappie.util;

import java.security.Permission;
import java.lang.SecurityManager;

public class MySecurityManager extends SecurityManager {
  @Override public void checkExit(int status) {
    throw new SecurityException();
  }

  @Override public void checkPermission(Permission perm) {
      // Allow other activities by default
  }
}
