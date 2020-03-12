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

  public static boolean doesImplement(CtClass cls, String interfaceName) {
    while (cls != null) {
      try {
        CtClass[] interfaces = cls.getInterfaces();
        for (int i = 0; i < interfaces.length; ++i)
          if (interfaces[i].getName().equals(interfaceName))
            return true;

      } catch(Exception exception) { }
      cls = getSuperClass(cls);
    }
    return false;
  }

  public static boolean isNative(String className) {
    return className.substring(0, 5).contains("java/") ||
           className.substring(0, 6).contains("javax/") ||
           className.contains("jdk/") ||
           className.contains("sun/");
  }
}
