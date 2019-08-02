package chappie.agent;

import java.lang.instrument.Instrumentation;

import java.util.ArrayList;

public class ChappieAgent {
  public static void premain(String agentArgs, Instrumentation inst) {
    inst.addTransformer(new ThreadOSMapper());

		try {
      inst.retransformClasses(new Class[] {java.lang.System.class});
     	inst.retransformClasses(new Class[] {java.lang.Thread.class});
		} catch(Exception exception) {
      System.out.println("unable to instrument java.lang.System and java.lang.Thread");
    }
	}
}
