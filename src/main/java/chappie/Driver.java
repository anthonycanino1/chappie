package chappie;

import chappie.profiling.Profile;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.util.List;
import java.util.Random;

import java.io.File;
import java.util.Arrays;

public class Driver {
  public static void main(String[] args) {

    Chappie.start();
    // TODO(timur): need a real benchmark to work with
    for (int i = 0; i < 1; i++) {
      long start = System.currentTimeMillis();
      try {
        Thread.sleep(500);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    Chappie.stop();

    // print vs write profiles; work needs to be done here as well
    for (Profile profile: Chappie.getProfiles()) {
      System.out.println(profile);
    }

    // try (FileWriter fw = new FileWriter("chappie-logs/log.txt")) {
    //   PrintWriter writer = new PrintWriter(fw);
    //   writer.println("start,end,socket,total,attributed");
    //   for (Profile profile: Chappie.getProfiles()) {
    //     writer.println(profile.dump());
    //   }
    // } catch (Exception e) {
    //   logger.info("couldn't open log file");
    // }
  }
}
