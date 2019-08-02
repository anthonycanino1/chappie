package chappie.agent;

import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.MalformedURLException;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.bytecode.Descriptor;
import javassist.CannotCompileException;

public class ThreadOSMapper implements ClassFileTransformer {

  static String mappingBody =
    "try {"                                                                       +
      "String thread = Thread.currentThread().getName())"                         +
      "System.out.println(\"Mapping \" + Thread.currentThread().getName());"      +
      "chappie.util.GLIBC.getThreadId();"                                         +
    "} catch (java.lang.NoClassDefFoundError ex) {"                               +
      "System.out.println(\"Couldn't map \" + Thread.currentThread().getName());" +
    "}";

  static CtClass getSuperClass(CtClass cls) {
    try {
      return cls.getSuperclass();
    } catch(Exception exception) {
      return null;
    }
  }

  static boolean isRunnable(CtClass cls) {
		CtClass klass = cls;
		while (klass != null) {
			try {
        CtClass[] interfaces = klass.getInterfaces();
  			for (int i = 0; i < interfaces.length; ++i)
  				if (interfaces[i].getName().equals("java.lang.Runnable"))
            return true;

      } catch(Exception exception) { }
			klass = getSuperClass(klass);
		}
		return false;
	}

	private ClassLoader classLoader;
  public ThreadOSMapper() {
    try {
      File dir = new File(System.getProperty("java.class.path"));
      URL url = dir.toURL();
      URL[] urls = new URL[] {url};
      classLoader = new URLClassLoader(urls);
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
  }

  public byte[] transform(
    ClassLoader loader,
    String className,
    Class classBeingRedefined,
    ProtectionDomain protectionDomain,
    byte[] classfileBuffer
  ) throws IllegalClassFormatException {
		try {
			ClassPool classPool = ClassPool.getDefault();
      CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));

      if(!ctClass.getPackageName().contains("java.") && isRunnable(ctClass)) {
        System.out.println("Modifying " + className);
  			CtMethod runMethod = ctClass.getMethod("run", Descriptor.ofMethod(CtClass.voidType, new CtClass[0]));
        try {
    			runMethod.insertBefore(mappingBody);
        } catch (CannotCompileException noBody) {
          throw noBody;
          System.out.println(className + " has no defined run body; adding empty dummy body");

          runMethod.setBody(";");
          runMethod.insertBefore(mappingBody);
        }
        byte[] byteCode = ctClass.toBytecode();
  			ctClass.detach();
        return byteCode;
      } else {
        return classfileBuffer;
      }
    } catch (Throwable ex) {
      System.out.println("Couldn't modify " + className);
      System.out.println(ex.getClass().getCanonicalName());

      return classfileBuffer;
    }
	}
}
