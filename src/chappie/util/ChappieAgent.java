package chappie.util;

import java.lang.instrument.Instrumentation;

import java.util.List;
import java.util.ArrayList;

public class ChappieAgent {
  public static void premain(String agentArgs, Instrumentation inst) {
		inst.addTransformer(new ThreadCoreMapInjector());

    inst.addTransformer(new ExitStopper(), true);
    inst.addTransformer(new ThreadCallsiteInjector(), true);

		try {
      inst.retransformClasses(new Class[] {java.lang.System.class, java.lang.Thread.class});
		} catch(Exception exception) { }
	}
}
