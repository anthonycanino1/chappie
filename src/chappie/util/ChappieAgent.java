package chappie.instrumentation;

import java.lang.instrument.Instrumentation;

public class ChappieAgent {
  public static void premain(String agentArgs, Instrumentation inst) {
		inst.addTransformer(new ThreadCoreMapInjector());
	}
}
