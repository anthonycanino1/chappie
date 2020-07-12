// package experiments;
//
// import static com.stoke.StochasticPolicyType.EPSILON_GREEDY_10;
// import static com.stoke.StochasticPolicyType.SOFTMAX;
// import static com.stoke.types.KnobValT.needInteger;
//
// import chappie.attribution.AttributionProfiler;
// import chappie.attribution.AttributionProfile;
// import chappie.util.LoggerUtil;
// import chappie.util.concurrent.SelfScheduledRunnable;
// import com.stoke.AeneasMachine;
// // import com.stoke.DiscreteKnob;
// import com.stoke.Knob;
// import com.stoke.types.KnobValT;
// import com.stoke.Reward;
// import java.util.ArrayList;
// import java.util.logging.Logger;
//
// public final class ExperimentController {
//   private static Logger logger;
//   private static ArrayList<AttributionProfile> profiles;
//   private static AttributionProfiler profiler;
//   private static AeneasMachine machine;
//   private static Thread controller;
//
//   private static ArrayList<Knob> knobs = new ArrayList<>();
//   private static Runnable controllerMethod = () -> { return; };
//
//   public static void start() {
//     LoggerUtil.setup();
//     logger = Logger.getLogger("chappie");
//
//     profiles = new ArrayList<>();
//     profiler = new AttributionProfiler(Integer.parseInt(System.getProperty("chappie.rate", "4")));
//
//     // DiscreteKnob knob = new DiscreteKnob("property", KnobValT.haveIntegers(0, 1, 2, 3, 4, 5));
//     machine = new AeneasMachine(
//       EPSILON_GREEDY_10,
//       knobs.toArray(Knob[]::new),
//       reward);
//
//     controller = new Thread(new SelfScheduledRunnable(controllerMethod, 512));
//
//     logger.info("starting controller");
//     profiler.start();
//     // machine.start();
//     // controller.start();
//   }
//
//   public static void stop() {
//     logger.info("stopping controller");
//
//     // try {
//     //   controller.interrupt();
//     //   controller.join();
//     // } catch (Exception e) {
//     // }
//     // machine.stop();
//     profiler.stop();
//
//     getProfiles();
//
//     profiler = null;
//     // machine = null;
//     // controller = null;
//   }
//
//   public static void addKnob(Knob knob) {
//     knobs.add(knob);
//   }
//
//   public static void addKnobs(Knob k1, Knob... ks) {
//     addKnob(k1);
//     for (Knob k: ks) {
//       addKnob(k);
//     }
//   }
//
//   public static void addController(Runnable r) {
//     controllerMethod = r;
//   }
//
//   public static Iterable<AttributionProfile> getProfiles() {
//     if (profiler != null) {
//       profiles = (ArrayList<AttributionProfile>) profiler.getProfiles();
//     }
//     return profiles;
//   }
//
//   public static void interact() {
//     if (machine != null) {
//       machine.interact();
//     }
//   }
//
//   public static int read(String knobName) {
//     if (machine != null) {
//       return KnobValT.needInteger(machine.read(knobName));
//     }
//
//     return Integer.MIN_VALUE;
//   }
//
//   private ChappieController() { }
// }
