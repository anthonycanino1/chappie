package chappie.agent;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.MalformedURLException;
import java.security.ProtectionDomain;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.bytecode.Descriptor;
import javassist.CannotCompileException;
import javassist.NotFoundException;

import chappie.agent.CtClassUtil;

import jlibc.libc;

public class TIDMapping implements ClassFileTransformer {

  private static String body = "chappie.agent.TIDMapping.mapTask();";
  public static void mapTask() {
    try {
      libc.getTaskId();
    } catch (Exception e) {
      System.out.println("couldn't map" + Thread.currentThread().getName());
    }
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
    try {
			ClassPool classPool = ClassPool.getDefault();
      CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));

      if (!className.contains("ZipFile") && !className.contains("java2d/Disposer") && CtClassUtil.doesImplement(ctClass, "java.lang.Runnable")) {
  			CtMethod runMethod = ctClass.getMethod("run", Descriptor.ofMethod(CtClass.voidType, new CtClass[0]));
        runMethod.insertBefore(body);

        byte[] byteCode = ctClass.toBytecode();
  			ctClass.detach();

        System.out.println("transformed " + className);

        return byteCode;
      } else {
        return classfileBuffer;
      }
    } catch (CannotCompileException | IOException | NotFoundException ex) {
      System.out.println("could not transform " + className);
      System.out.println(ex.getClass().getCanonicalName() + ": " + ex.getMessage());
      // ex.printStackTrace();

      return classfileBuffer;
    }
	}
}
