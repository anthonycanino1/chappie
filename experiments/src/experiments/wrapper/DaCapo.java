// package experiments.wrapper;
//
// import chappie.ChappieController;
// import chappie.attribution.AttributionProfile;
// import java.io.PrintWriter;
// import java.io.FileWriter;
// import java.util.ArrayList;
// import java.util.logging.Logger;
// import org.dacapo.harness.Callback;
// import org.dacapo.harness.CommandLineArgs;
//
// public class DaCapo extends Callback {
//   public DaCapo(CommandLineArgs args) {
//     super(args);
//   }
//
//   @Override
//   public void start(String benchmark) {
//     ChappieController.start();
//     super.start(benchmark);
//   }
//
//   @Override
//   public void stop(long duration) {
//     super.stop(duration);
//     ChappieController.stop();
//
//     Logger logger = Logger.getLogger("chappie");
//     for (AttributionProfile profile: ChappieController.getProfiles()) {
//       logger.info(profile.toString());
//     }
//
//   //   String suffix = System.getProperty("chappie.filepath", "0");
//   //   try (FileWriter fw = new FileWriter("chappie-logs/log-" + suffix + ".txt")) {
//   //     PrintWriter writer = new PrintWriter(fw);
//   //     writer.println("start,end,socket,total,attributed");
//   //     for (Profile profile: profiles) {
//   //       writer.println(profile.dump());
//   //     }
//   //   } catch (Exception e) {
//   //     System.out.println("couldn't open log file");
//   //   }
//   }
// }
