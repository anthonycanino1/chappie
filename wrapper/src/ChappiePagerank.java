package chappie_util.graphchi;

import chappie.input.Config;
import chappie.SleepingChaperone;

import edu.cmu.graphchi.apps.Pagerank;

import edu.cmu.graphchi.*;
import edu.cmu.graphchi.datablocks.FloatConverter;
import edu.cmu.graphchi.engine.GraphChiEngine;
import edu.cmu.graphchi.engine.VertexInterval;
import edu.cmu.graphchi.io.CompressedIO;
import edu.cmu.graphchi.preprocessing.EdgeProcessor;
import edu.cmu.graphchi.preprocessing.FastSharder;
import edu.cmu.graphchi.preprocessing.VertexIdTranslate;
import edu.cmu.graphchi.preprocessing.VertexProcessor;
import edu.cmu.graphchi.util.IdFloat;
import edu.cmu.graphchi.util.Toplist;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.TreeSet;
import java.util.logging.Logger;

public class ChappiePagerank extends Pagerank {
  private static Logger logger = ChiLogger.getLogger("chappie pagerank");
  //
  // Config config;
  // SleepingChaperone chaperone;
  //
  // public ChappiePagerank() {
  //   super();
  //
  //   String configPath = System.getProperty("chappie.config", null);
  //   String workDir = System.getProperty("chappie.workDir", null);
  //   if (configPath == null) {
  //     System.out.println("no config found");
  //     System.exit(0);
  //   }
  //
  //   config = Config.readConfig(configPath, workDir);
  //   System.out.println(config.toString());
  // }
  //
  // int iter = 0;
  // public void beginIteration(GraphChiContext ctx) {
  //   System.setProperty("chappie.suffix", Integer.toString(iter++));
  //   chaperone = new SleepingChaperone(config);
  //   super.beginIteration(ctx);
  // }
  //
  // public void endIteration(GraphChiContext ctx) {
  //   super.endIteration(ctx);
  //   chaperone.cancel();
  // }

  // Have to use this template due to the call to Pagerank specifically
  public static void main(String[] args) throws  Exception {
    String baseFilename = args[0];
    int nShards = Integer.parseInt(args[1]);
    int iters = Integer.parseInt(args[2]);
    String fileType = (args.length >= 4 ? args[3] : null);

    CompressedIO.disableCompression();

    /* Create shards */
    FastSharder sharder = Pagerank.createSharder(baseFilename, nShards);
    if (baseFilename.equals("pipein")) {
      sharder.shard(System.in, fileType);
    } else {
      if (!new File(ChiFilenames.getFilenameIntervals(baseFilename, nShards)).exists()) {
        sharder.shard(new FileInputStream(new File(baseFilename)), fileType);
      } else {
        logger.info("Found shards -- no need to preprocess");
      }
    }

    String configPath = System.getProperty("chappie.config", null);
    String workDir = System.getProperty("chappie.workDir", null);
    if (configPath == null) {
      System.out.println("no config found");
      System.exit(0);
    }

    Config config = Config.readConfig(configPath, workDir);
    System.out.println(config.toString());

    // GraphChiEngine<Float, Float> engine = new GraphChiEngine<Float, Float>(baseFilename, nShards);
    // engine.setEdataConverter(new FloatConverter());
    // engine.setVertexDataConverter(new FloatConverter());
    // engine.setModifiesInedges(false); // Important optimization
    //
    // engine.run(new ChappiePagerank(), iters);

    for (int i = 0; i < iters; i++) {
      System.out.println("################## Starting iter " + (i + 1) + " ##################");
      /* Run GraphChi */
      GraphChiEngine<Float, Float> engine = new GraphChiEngine<Float, Float>(baseFilename, nShards);
      engine.setMemoryBudgetMb(1000);
      engine.setEdataConverter(new FloatConverter());
      engine.setVertexDataConverter(new FloatConverter());
      engine.setModifiesInedges(false); // Important optimization

      System.setProperty("chappie.suffix", Integer.toString(i));
      SleepingChaperone chaperone = new SleepingChaperone(config);
      engine.run(new ChappiePagerank(), 5);
      chaperone.cancel();
    }

    logger.info("Ready.");
  }
}
