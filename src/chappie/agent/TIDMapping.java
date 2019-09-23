package chappie.agent;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.MalformedURLException;
import java.security.ProtectionDomain;
import java.util.logging.Logger;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.bytecode.Descriptor;
import javassist.CannotCompileException;

import chappie.util.ChappieLogger;

public class TIDMapping implements ClassFileTransformer {

  static String mappingBody = "chappie.glibc.GLIBC.getTaskId();";

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
  public TIDMapping() {
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
    Logger logger = ChappieLogger.getLogger();
		try {
			ClassPool classPool = ClassPool.getDefault();
      CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));

      if(isRunnable(ctClass)) {
  			CtMethod runMethod = ctClass.getMethod("run", Descriptor.ofMethod(CtClass.voidType, new CtClass[0]));
        runMethod.insertBefore(mappingBody);

        byte[] byteCode = ctClass.toBytecode();
  			ctClass.detach();

        logger.info("transformed " + className);

        return byteCode;
      } else {
        return classfileBuffer;
      }
    } catch (Throwable ex) {
      logger.info("could not transform " + className);
      logger.info(ex.getClass().getCanonicalName() + ": " + ex.getMessage());
      // ex.printStackTrace();

      return classfileBuffer;
    }
	}
}
