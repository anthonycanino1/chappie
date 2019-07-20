package chappie.util;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.ClassFileTransformer;

import java.util.List;
import java.util.ArrayList;

public class ChappieAgent {
  public static void premain(String agentArgs, Instrumentation inst) {
    System.out.println("Reaching Agent ...... Yeeeeeeeeee");
    inst.addTransformer(new ThreadOSMapper());

		try {
     			 inst.retransformClasses(new Class[] {java.lang.Thread.class});
		} catch(Exception exception) {
      System.out.println("?");
    }
	}
}
