package experiments.wrapper;

import static com.stoke.StochasticPolicyType.EPSILON_GREEDY_10;

import com.stoke.StochasticPolicyType;

import chappie.Chappie;
import chappie.concurrent.SelfScheduledRunnable;
import chappie.util.LoggerUtil;

import com.stoke.AeneasMachine;
import com.stoke.DiscreteKnob;
import com.stoke.Knob;
import com.stoke.types.KnobValT;

import experiments.processing.ExperimentEnergyAttribution;
import experiments.ExperimentProfiler;
import experiments.ExperimentReward;
import experiments.processing.ExperimentEnergyAttribution;

import java.io.FileWriter;
import java.io.PrintWriter;

import java.time.Instant;

import java.util.ArrayList;
import java.util.HashMap;

import org.sunflow.core.Display;
import org.sunflow.image.Color;
import org.sunflow.math.Matrix4;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;
import org.sunflow.system.BenchmarkTest;
import org.sunflow.SunflowAPI;

public class SunflowWrapper implements BenchmarkTest {
    private int resolution;
    private int threads;

    public static void main(String[] args) {
      LoggerUtil.setup();
      ExperimentProfiler profiler = new ExperimentProfiler(4);

      boolean useAeneas = Boolean.parseBoolean(System.getProperty("aeneas", "true"));

      String[] properties = {"aa.min", "aa.max"};
      Knob[] knobs = new Knob[] {
        new DiscreteKnob("aa.min", KnobValT.haveIntegers(-1, -2, -3)),
        new DiscreteKnob("aa.max", KnobValT.haveIntegers(1, 2, 3))
      };
      ExperimentReward reward = new ExperimentReward(profiler);
      AeneasMachine machine = new AeneasMachine(
        StochasticPolicyType.toStochasticPolicy(System.getProperty("aeneas.policy", "EPSILON_GREEDY_10")),
        knobs,
        reward);
      HashMap<String, Integer> lastProperties = new HashMap<String, Integer>() {{
        put("aa.min", -1);
        put("aa.max", 1);
      }};
      // Thread controller = new Thread(new SelfScheduledRunnable(
      //   () -> {
      //     try {
      //       while (useAeneas && reward.check()) {
      //         machine.interact();
      //       }
      //       if (useAeneas) {
      //         for (String property : properties) {
      //           int val = KnobValT.needInteger(machine.read(property));
      //           if (val != lastProperties.get(property)) {
      //             parameter(property, val);
      //             lastProperties.put(property, val);
      //           }
      //         }
      //       }
      //     } catch (Exception e) {
      //       System.out.println("couldn't interact!");
      //       e.printStackTrace();
      //     }
      //   },
      //   256));

      ArrayList<Instant> timestamps = new ArrayList<>();
      timestamps.add(Instant.now());
      for (int i = 0; i < 100; i++) {
        if (i == 10) {
          profiler.start();
          if (useAeneas) {
            machine.start();
            // controller.start();
          }
        }
        new SunflowWrapper(64, 40).kernelMain();
        timestamps.add(Instant.now());
        if (useAeneas && i >= 10) {
          // System.setProperty("aa.max", "3");
          machine.interact();
          // while (reward.check()) {
          // }
          for (String property : properties) {
            int val = KnobValT.needInteger(machine.read(property));
            if (val != lastProperties.get(property)) {
              System.setProperty(property, Integer.toString(val));
              lastProperties.put(property, val);
            }
          }
        }
      }

      profiler.stop();
      if (useAeneas) {
        machine.stop();
        // try {
        //   controller.interrupt();
        //   controller.join();
        // } catch (Exception e) { }
      }

      // for (ExperimentEnergyAttribution profile: profiler.getProfiles()) {
      //   if (profile.getApplicationEnergy() > 0) {
      //     System.out.println(profile);
      //   }
      // }

      String path = System.getProperty("chappie.out", "chappie-log.txt");
      try (PrintWriter writer = new PrintWriter(new FileWriter(path))) {
        writer.println("start,end,socket,total,attributed");
        for (ExperimentEnergyAttribution profile: profiler.getProfiles()) {
          writer.println(profile.dump());
        }
      } catch (Exception e) {
        System.out.println("unable to write profiles");
        e.printStackTrace();
      }

      try (PrintWriter writer = new PrintWriter(new FileWriter(path + ".stamp"))) {
        writer.println("timestamp");
        for (Instant stamp: timestamps) {
          writer.println(stamp.toEpochMilli());
        }
      } catch (Exception e) {
        System.out.println("unable to write stamps");
        e.printStackTrace();
      }
    }

    public SunflowWrapper(int resolution, int threads) {
      this.resolution = resolution;
      this.threads = threads;
    }

    public void kernelMain() {
      new BenchmarkScene();
    }

    private class BenchmarkScene extends SunflowAPI {
        public BenchmarkScene() {
            build();

            Display dummyDisplay = new Display() {
              public void imageBegin(int w, int h, int bucketSize) { }
              public void imagePrepare(int x, int y, int w, int h, int id) { }
              public void imageUpdate(int x, int y, int w, int h, Color[] data, float[] alpha) { }
              public void imageFill(int x, int y, int w, int h, Color c, float alpha) { }
              public void imageEnd() { }
            };

            // LoggerUtil.setup();
            // ExperimentProfiler profiler = new ExperimentProfiler(4);
            //
            // boolean useAeneas = Boolean.parseBoolean(System.getProperty("aeneas", "true"));
            //
            // String[] properties = {"aa.min", "aa.max"};
            // Knob[] knobs = new Knob[] {
            //   new DiscreteKnob("aa.min", KnobValT.haveIntegers(-3, -2, -1)),
            //   new DiscreteKnob("aa.max", KnobValT.haveIntegers(1, 2, 3))
            // };
            // ExperimentReward reward = new ExperimentReward(profiler);
            // AeneasMachine machine = new AeneasMachine(
            //   StochasticPolicyType.toStochasticPolicy(System.getProperty("aeneas.policy", "EPSILON_GREEDY_10")),
            //   knobs,
            //   reward);
            // HashMap<String, Integer> lastProperties = new HashMap<String, Integer>() {{
            //   put("aa.min", -1);
            //   put("aa.max", 1);
            // }};
            // // Thread controller = new Thread(new SelfScheduledRunnable(
            // //   () -> {
            // //     try {
            // //       while (useAeneas && reward.check()) {
            // //         machine.interact();
            // //       }
            // //       if (useAeneas) {
            // //         for (String property : properties) {
            // //           int val = KnobValT.needInteger(machine.read(property));
            // //           if (val != lastProperties.get(property)) {
            // //             parameter(property, val);
            // //             lastProperties.put(property, val);
            // //           }
            // //         }
            // //       }
            // //     } catch (Exception e) {
            // //       System.out.println("couldn't interact!");
            // //       e.printStackTrace();
            // //     }
            // //   },
            // //   256));
            //
            // ArrayList<Instant> timestamps = new ArrayList<>();
            // timestamps.add(Instant.now());
            // for (int i = 0; i < 50; i++) {
            //   if (i == 5) {
            //     profiler.start();
            //     if (useAeneas) {
            //       // machine.start();
            //       // controller.start();
            //     }
            //   }
              render(SunflowAPI.DEFAULT_OPTIONS, dummyDisplay);
            //   timestamps.add(Instant.now());
            //   if (useAeneas && i >= 5) {
            //     parameter("aa.max", 3);
            //     // machine.interact();
            //     // // while (reward.check()) {
            //     // // }
            //     // for (String property : properties) {
            //     //   int val = KnobValT.needInteger(machine.read(property));
            //     //   if (val != lastProperties.get(property)) {
            //     //     parameter(property, val);
            //     //     lastProperties.put(property, val);
            //     //   }
            //     // }
            //   }
            // }
            //
            // profiler.stop();
            // if (useAeneas) {
            //   // machine.stop();
            //   // try {
            //   //   controller.interrupt();
            //   //   controller.join();
            //   // } catch (Exception e) { }
            // }
            //
            // // for (ExperimentEnergyAttribution profile: profiler.getProfiles()) {
            // //   if (profile.getApplicationEnergy() > 0) {
            // //     System.out.println(profile);
            // //   }
            // // }
            //
            // String path = System.getProperty("chappie.out", "chappie-log.txt");
            // try (PrintWriter writer = new PrintWriter(new FileWriter(path))) {
            //   writer.println("start,end,socket,total,attributed");
            //   for (ExperimentEnergyAttribution profile: profiler.getProfiles()) {
            //     writer.println(profile.dump());
            //   }
            // } catch (Exception e) {
            //   System.out.println("unable to write profiles");
            //   e.printStackTrace();
            // }
            //
            // try (PrintWriter writer = new PrintWriter(new FileWriter(path + ".stamp"))) {
            //   writer.println("timestamp");
            //   for (Instant stamp: timestamps) {
            //     writer.println(stamp.toEpochMilli());
            //   }
            // } catch (Exception e) {
            //   System.out.println("unable to write profiles");
            //   e.printStackTrace();
            // }
        }

        @Override
        public void build() {
            // settings
            parameter("threads", threads);
            // spawn regular priority threads
            parameter("threads.lowPriority", false);
            parameter("resolutionX", resolution);
            parameter("resolutionY", resolution);
            parameter("aa.min", Integer.parseInt(System.getProperty("aa.min", "-1")));
            parameter("aa.max", Integer.parseInt(System.getProperty("aa.max", "1")));
            parameter("filter", "triangle");
            parameter("depths.diffuse", 2);
            parameter("depths.reflection", 2);
            parameter("depths.refraction", 2);
            parameter("bucket.order", "hilbert");
            parameter("bucket.size", 32);
            // gi options
            parameter("gi.engine", "igi");
            parameter("gi.igi.samples", 90);
            parameter("gi.igi.c", 0.000008f);
            options(SunflowAPI.DEFAULT_OPTIONS);
            buildCornellBox();
        }

        private void buildCornellBox() {
            // camera
            parameter("transform", Matrix4.lookAt(new Point3(0, 0, -600), new Point3(0, 0, 0), new Vector3(0, 1, 0)));
            parameter("fov", 45.0f);
            camera("main_camera", "pinhole");
            parameter("camera", "main_camera");
            options(SunflowAPI.DEFAULT_OPTIONS);
            // cornell box
            float minX = -200;
            float maxX = 200;
            float minY = -160;
            float maxY = minY + 400;
            float minZ = -250;
            float maxZ = 200;

            float[] verts = new float[] { minX, minY, minZ, maxX, minY, minZ,
                    maxX, minY, maxZ, minX, minY, maxZ, minX, maxY, minZ, maxX,
                    maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ, };
            int[] indices = new int[] { 0, 1, 2, 2, 3, 0, 4, 5, 6, 6, 7, 4, 1,
                    2, 5, 5, 6, 2, 2, 3, 6, 6, 7, 3, 0, 3, 4, 4, 7, 3 };

            parameter("diffuse", null, 0.70f, 0.70f, 0.70f);
            shader("gray_shader", "diffuse");
            parameter("diffuse", null, 0.80f, 0.25f, 0.25f);
            shader("red_shader", "diffuse");
            parameter("diffuse", null, 0.25f, 0.25f, 0.80f);
            shader("blue_shader", "diffuse");

            // build walls
            parameter("triangles", indices);
            parameter("points", "point", "vertex", verts);
            parameter("faceshaders", new int[] { 0, 0, 0, 0, 1, 1, 0, 0, 2, 2 });
            geometry("walls", "triangle_mesh");

            // instance walls
            parameter("shaders", new String[] { "gray_shader", "red_shader",
                    "blue_shader" });
            instance("walls.instance", "walls");

            // create mesh light
            parameter("points", "point", "vertex", new float[] { -50, maxY - 1,
                    -50, 50, maxY - 1, -50, 50, maxY - 1, 50, -50, maxY - 1, 50 });
            parameter("triangles", new int[] { 0, 1, 2, 2, 3, 0 });
            parameter("radiance", null, 15, 15, 15);
            parameter("samples", 8);
            light("light", "triangle_mesh");

            // spheres
            parameter("eta", 1.6f);
            shader("Glass", "glass");
            sphere("glass_sphere", "Glass", -120, minY + 55, -150, 50);
            parameter("color", null, 0.70f, 0.70f, 0.70f);
            shader("Mirror", "mirror");
            sphere("mirror_sphere", "Mirror", 100, minY + 60, -50, 50);

            // scanned model
            geometry("teapot", "teapot");
            parameter("transform", Matrix4.translation(80, -50, 100).multiply(Matrix4.rotateX((float) -Math.PI / 6)).multiply(Matrix4.rotateY((float) Math.PI / 4)).multiply(Matrix4.rotateX((float) -Math.PI / 2).multiply(Matrix4.scale(1.2f))));
            parameter("shaders", "gray_shader");
            instance("teapot.instance1", "teapot");
            parameter("transform", Matrix4.translation(-80, -160, 50).multiply(Matrix4.rotateY((float) Math.PI / 4)).multiply(Matrix4.rotateX((float) -Math.PI / 2).multiply(Matrix4.scale(1.2f))));
            parameter("shaders", "gray_shader");
            instance("teapot.instance2", "teapot");
        }

        private void sphere(String name, String shaderName, float x, float y, float z, float radius) {
            geometry(name, "sphere");
            parameter("transform", Matrix4.translation(x, y, z).multiply(Matrix4.scale(radius)));
            parameter("shaders", shaderName);
            instance(name + ".instance", name);
        }
    }

    public void kernelBegin() { }

    public void kernelEnd() { }
}
