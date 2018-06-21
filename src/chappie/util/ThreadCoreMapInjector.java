package chappie.instrumentation;

import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.bytecode.Descriptor;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.MalformedURLException;

public class ThreadCoreMapInjector implements ClassFileTransformer {

  public static CtClass getSuperClass(CtClass cls) {
    try {
      return cls.getSuperclass();
    } catch(Exception exception) {
      return null;
    }
  }

  public static boolean isRunnable(CtClass cls) {
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

  public ThreadCoreMapInjector() {
    try {
      File dir = new File(System.getProperty("java.class.path"));
      URL url = dir.toURL();
      URL[] urls = new URL[] {url};
      classLoader = new URLClassLoader(urls);
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
  }

  public byte[] transform(ClassLoader loader, String className,	Class classBeingRedefined,
                          ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

		if (className.startsWith("java/") || className.startsWith("javax/") || className.startsWith("sun/") ||
			   className.startsWith("org/jikesrvm") || className.startsWith("javassist"))
			return classfileBuffer;

		byte[] byteCode = classfileBuffer;

		try {
			ClassPool classPool = ClassPool.getDefault();
			CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));

			if(isRunnable(ctClass)) {
				CtMethod runMethod = ctClass.getMethod("run", Descriptor.ofMethod(CtClass.voidType, new CtClass[0]));
				runMethod.insertBefore("chappie.util.GLIBC.getThreadId();");
			} else {
				return classfileBuffer;
			}

			byteCode = ctClass.toBytecode();
			ctClass.detach();
		} catch (Throwable ex) {
			System.out.println("Couldn't modify " + className);
		}

		return byteCode;
	}
}
