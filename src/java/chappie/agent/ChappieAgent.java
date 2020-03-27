package chappie.agent;

import java.lang.instrument.Instrumentation;
// import java.util.logging.Logger;

// import chappie.util.ChappieLogger;

public class ChappieAgent {
  public static void premain(String agentArgs, Instrumentation inst) {
    inst.addTransformer(new TIDMapping());

		try {
      inst.retransformClasses(new Class[] {java.lang.System.class});
		} catch(Exception exception) { }

    try {
      inst.retransformClasses(new Class[] {java.lang.Thread.class});
		} catch(Exception exception) { }
	}
}
