package chappie.agent;

import javassist.CtClass;

public class CtClassUtil {
  private static CtClass getSuperClass(CtClass cls) {
    try {
      return cls.getSuperclass();
    } catch(Exception exception) {
      return null;
    }
  }

  public static boolean doesImplement(CtClass cls, String intrfc) {
    CtClass klass = cls;
    while (klass != null) {
      try {
        CtClass[] interfaces = klass.getInterfaces();
        for (int i = 0; i < interfaces.length; ++i)
          if (interfaces[i].getName().equals(intrfc))
            return true;

      } catch(Exception exception) { }
      klass = getSuperClass(klass);
    }
    return false;
  }
}
