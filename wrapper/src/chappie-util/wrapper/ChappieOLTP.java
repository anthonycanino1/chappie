package chappie_util.oltp;

import chappie.input.Config;
import chappie.SleepingChaperone;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.collections15.map.ListOrderedMap;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.apache.log4j.Logger;

import com.oltpbenchmark.*;
import com.oltpbenchmark.api.BenchmarkModule;
import com.oltpbenchmark.api.TransactionType;
import com.oltpbenchmark.api.TransactionTypes;
import com.oltpbenchmark.api.Worker;
import com.oltpbenchmark.types.DatabaseType;
import com.oltpbenchmark.util.ClassUtil;
import com.oltpbenchmark.util.FileUtil;
import com.oltpbenchmark.util.QueueLimitException;
import com.oltpbenchmark.util.ResultUploader;
import com.oltpbenchmark.util.StringBoxUtil;
import com.oltpbenchmark.util.StringUtil;
import com.oltpbenchmark.util.TimeUtil;

public class ChappieOLTP {
    private static final Logger LOG = Logger.getLogger(DBWorkload.class);

    private static final String SINGLE_LINE = StringUtil.repeat("=", 70);

    private static final String RATE_DISABLED = "disabled";
    private static final String RATE_UNLIMITED = "unlimited";

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        // Initialize log4j
        String log4jPath = System.getProperty("log4j.configuration");
        if (log4jPath != null) {
            org.apache.log4j.PropertyConfigurator.configure(log4jPath);
        } else {
            throw new RuntimeException("Missing log4j.properties file");
        }

        if (ClassUtil.isAssertsEnabled()) {
            LOG.warn("\n" + getAssertWarning());
        }

        // create the command line parser
        CommandLineParser parser = new PosixParser();
        XMLConfiguration pluginConfig=null;
        try {
            pluginConfig = new XMLConfiguration("config/plugin.xml");
        } catch (ConfigurationException e1) {
            LOG.info("Plugin configuration file config/plugin.xml is missing");
            e1.printStackTrace();
        }
        pluginConfig.setExpressionEngine(new XPathExpressionEngine());
        Options options = new Options();
        options.addOption(
                "b",
                "bench",
                true,
                "[required] Benchmark class. Currently supported: "+ pluginConfig.getList("/plugin//@name"));
        options.addOption(
                "c",
                "config",
                true,
                "[required] Workload configuration file");
        options.addOption(
                null,
                "iters",
                true,
                "Run the benchmark iters times");

        options.addOption("v", "verbose", false, "Display Messages");
        options.addOption("h", "help", false, "Print this help");

        // parse the command line arguments
        CommandLine argsLine = parser.parse(options, args);
        if (argsLine.hasOption("h")) {
            printUsage(options);
            return;
        } else if (argsLine.hasOption("c") == false) {
            LOG.error("Missing Configuration file");
            printUsage(options);
            return;
        } else if (argsLine.hasOption("b") == false) {
            LOG.fatal("Missing Benchmark Class to load");
            printUsage(options);
            return;
        }

        // Seconds
        int intervalMonitor = 0;

        // -------------------------------------------------------------------
        // GET PLUGIN LIST
        // -------------------------------------------------------------------

        String targetBenchmarks = argsLine.getOptionValue("b");
        String[] targetList = targetBenchmarks.split(",");

        // Use this list for filtering of the output

        String configFile = argsLine.getOptionValue("c");
        XMLConfiguration xmlConfig = new XMLConfiguration(configFile);
        xmlConfig.setExpressionEngine(new XPathExpressionEngine());

        // Load the configuration for each benchmark
        int lastTxnId = 0;

        @Deprecated
        boolean verbose = argsLine.hasOption("v");

        int iters = Integer.parseInt(argsLine.getOptionValue("iters"));

        String configPath = System.getProperty("chappie.config", null);
        String workDir = System.getProperty("chappie.workDir", null);

        if (configPath == null) {
          System.out.println("no config found");
          System.exit(0);
        }

        Config config = Config.readConfig(configPath, workDir);
        System.out.println(iters);
        System.out.println(config.toString());

        for (int k = 0; k < iters; k++) {
          List<BenchmarkModule> benchList = new ArrayList<BenchmarkModule>();
          List<TransactionType> activeTXTypes = new ArrayList<TransactionType>();
          for (String plugin : targetList) {
            String pluginTest = "[@bench='" + plugin + "']";

            WorkloadConfiguration wrkld = new WorkloadConfiguration();
            wrkld.setBenchmarkName(plugin);
            wrkld.setXmlConfig(xmlConfig);
            boolean scriptRun = false;

            // Pull in database configuration
            wrkld.setDBType(DatabaseType.get(xmlConfig.getString("dbtype")));
            wrkld.setDBDriver(xmlConfig.getString("driver"));
            wrkld.setDBConnection(xmlConfig.getString("DBUrl"));
            wrkld.setDBName(xmlConfig.getString("DBName"));
            wrkld.setDBUsername(xmlConfig.getString("username"));
            wrkld.setDBPassword(xmlConfig.getString("password"));

            int terminals = xmlConfig.getInt("terminals[not(@bench)]", 0);
            terminals = xmlConfig.getInt("terminals" + pluginTest, terminals);
            wrkld.setTerminals(terminals);

            if (xmlConfig.containsKey("loaderThreads")) {
                int loaderThreads = xmlConfig.getInt("loaderThreads");
                wrkld.setLoaderThreads(loaderThreads);
            }

            String isolationMode = xmlConfig.getString("isolation[not(@bench)]", "TRANSACTION_SERIALIZABLE");
            wrkld.setIsolationMode(xmlConfig.getString("isolation" + pluginTest, isolationMode));
            wrkld.setScaleFactor(xmlConfig.getDouble("scalefactor", 1.0));
            wrkld.setRecordAbortMessages(xmlConfig.getBoolean("recordabortmessages", false));
            wrkld.setDataDir(xmlConfig.getString("datadir", "."));

            double selectivity = -1;
            try {
                selectivity = xmlConfig.getDouble("selectivity");
                wrkld.setSelectivity(selectivity);
            }
            catch(NoSuchElementException nse) {
                // Nothing to do here !
            }

            // ----------------------------------------------------------------
            // CREATE BENCHMARK MODULE
            // ----------------------------------------------------------------

            String classname = pluginConfig.getString("/plugin[@name='" + plugin + "']");

            if (classname == null)
                throw new ParseException("Plugin " + plugin + " is undefined in config/plugin.xml");
            BenchmarkModule bench = ClassUtil.newInstance(classname,
                                                          new Object[] { wrkld },
                                                          new Class<?>[] { WorkloadConfiguration.class });
            Map<String, Object> initDebug = new ListOrderedMap<String, Object>();
            initDebug.put("Benchmark", String.format("%s {%s}", plugin.toUpperCase(), classname));
            initDebug.put("Configuration", configFile);
            initDebug.put("Type", wrkld.getDBType());
            initDebug.put("Driver", wrkld.getDBDriver());
            initDebug.put("URL", wrkld.getDBConnection());
            initDebug.put("Isolation", wrkld.getIsolationString());
            initDebug.put("Scale Factor", wrkld.getScaleFactor());

            if(selectivity != -1)
                initDebug.put("Selectivity", selectivity);

            LOG.info(SINGLE_LINE + "\n\n" + StringUtil.formatMaps(initDebug));
            LOG.info(SINGLE_LINE);

            // ----------------------------------------------------------------
            // LOAD TRANSACTION DESCRIPTIONS
            // ----------------------------------------------------------------
            int numTxnTypes = xmlConfig.configurationsAt("transactiontypes" + pluginTest + "/transactiontype").size();
            if (numTxnTypes == 0 && targetList.length == 1) {
                //if it is a single workload run, <transactiontypes /> w/o attribute is used
                pluginTest = "[not(@bench)]";
                numTxnTypes = xmlConfig.configurationsAt("transactiontypes" + pluginTest + "/transactiontype").size();
            }
            wrkld.setNumTxnTypes(numTxnTypes);

            List<TransactionType> ttypes = new ArrayList<TransactionType>();
            ttypes.add(TransactionType.INVALID);
            int txnIdOffset = lastTxnId;
            for (int i = 1; i <= wrkld.getNumTxnTypes(); i++) {
                String key = "transactiontypes" + pluginTest + "/transactiontype[" + i + "]";
                String txnName = xmlConfig.getString(key + "/name");

                // Get ID if specified; else increment from last one.
                int txnId = i;
                if (xmlConfig.containsKey(key + "/id")) {
                    txnId = xmlConfig.getInt(key + "/id");
                }

                TransactionType tmpType = bench.initTransactionType(txnName, txnId + txnIdOffset);

                // Keep a reference for filtering
                activeTXTypes.add(tmpType);

                // Add a ref for the active TTypes in this benchmark
                ttypes.add(tmpType);
                lastTxnId = i;
            } // FOR

            // Wrap the list of transactions and save them
            TransactionTypes tt = new TransactionTypes(ttypes);
            wrkld.setTransTypes(tt);
            LOG.debug("Using the following transaction types: " + tt);

            // Read in the groupings of transactions (if any) defined for this
            // benchmark
            HashMap<String,List<String>> groupings = new HashMap<String,List<String>>();
            int numGroupings = xmlConfig.configurationsAt("transactiontypes" + pluginTest + "/groupings/grouping").size();
            LOG.debug("Num groupings: " + numGroupings);
            for (int i = 1; i < numGroupings + 1; i++) {
                String key = "transactiontypes" + pluginTest + "/groupings/grouping[" + i + "]";

                // Get the name for the grouping and make sure it's valid.
                String groupingName = xmlConfig.getString(key + "/name").toLowerCase();
                if (!groupingName.matches("^[a-z]\\w*$")) {
                    LOG.fatal(String.format("Grouping name \"%s\" is invalid."
                                + " Must begin with a letter and contain only"
                                + " alphanumeric characters.", groupingName));
                    System.exit(-1);
                }
                else if (groupingName.equals("all")) {
                    LOG.fatal("Grouping name \"all\" is reserved."
                              + " Please pick a different name.");
                    System.exit(-1);
                }

                // Get the weights for this grouping and make sure that there
                // is an appropriate number of them.
                List<String> groupingWeights = xmlConfig.getList(key + "/weights");
                if (groupingWeights.size() != numTxnTypes) {
                    LOG.fatal(String.format("Grouping \"%s\" has %d weights,"
                                + " but there are %d transactions in this"
                                + " benchmark.", groupingName,
                                groupingWeights.size(), numTxnTypes));
                    System.exit(-1);
                }

                LOG.debug("Creating grouping with name, weights: " + groupingName + ", " + groupingWeights);
                groupings.put(groupingName, groupingWeights);
            }

            // All benchmarks should also have an "all" grouping that gives
            // even weight to all transactions in the benchmark.
            List<String> weightAll = new ArrayList<String>();
            for (int i = 0; i < numTxnTypes; ++i)
                weightAll.add("1");
            groupings.put("all", weightAll);
            benchList.add(bench);

            // ----------------------------------------------------------------
            // WORKLOAD CONFIGURATION
            // ----------------------------------------------------------------

            int size = xmlConfig.configurationsAt("/works/work").size();
            for (int i = 1; i < size + 1; i++) {
                SubnodeConfiguration work = xmlConfig.configurationAt("works/work[" + i + "]");
                List<String> weight_strings;

                // use a workaround if there multiple workloads or single
                // attributed workload
                if (targetList.length > 1 || work.containsKey("weights[@bench]")) {
                    String weightKey = work.getString("weights" + pluginTest).toLowerCase();
                    if (groupings.containsKey(weightKey))
                        weight_strings = groupings.get(weightKey);
                    else
                    weight_strings = getWeights(plugin, work);
                } else {
                    String weightKey = work.getString("weights[not(@bench)]").toLowerCase();
                    if (groupings.containsKey(weightKey))
                        weight_strings = groupings.get(weightKey);
                    else
                    weight_strings = work.getList("weights[not(@bench)]");
                }
                int rate = 1;
                boolean rateLimited = true;
                boolean disabled = false;
                boolean serial = false;
                boolean timed = false;

                // can be "disabled", "unlimited" or a number
                String rate_string;
                rate_string = work.getString("rate[not(@bench)]", "");
                rate_string = work.getString("rate" + pluginTest, rate_string);
                if (rate_string.equals(RATE_DISABLED)) {
                    disabled = true;
                } else if (rate_string.equals(RATE_UNLIMITED)) {
                    rateLimited = false;
                } else if (rate_string.isEmpty()) {
                    LOG.fatal(String.format("Please specify the rate for phase %d and workload %s", i, plugin));
                    System.exit(-1);
                } else {
                    try {
                        rate = Integer.parseInt(rate_string);
                        if (rate < 1) {
                            LOG.fatal("Rate limit must be at least 1. Use unlimited or disabled values instead.");
                            System.exit(-1);
                        }
                    } catch (NumberFormatException e) {
                        LOG.fatal(String.format("Rate string must be '%s', '%s' or a number", RATE_DISABLED, RATE_UNLIMITED));
                        System.exit(-1);
                    }
                }
                Phase.Arrival arrival=Phase.Arrival.REGULAR;
                String arrive=work.getString("@arrival","regular");
                if(arrive.toUpperCase().equals("POISSON"))
                    arrival=Phase.Arrival.POISSON;

                // We now have the option to run all queries exactly once in
                // a serial (rather than random) order.
                String serial_string;
                serial_string = work.getString("serial", "false");
                if (serial_string.equals("true")) {
                    serial = true;
                }
                else if (serial_string.equals("false")) {
                    serial = false;
                }
                else {
                    LOG.fatal("Serial string should be either 'true' or 'false'.");
                    System.exit(-1);
                }

                // We're not actually serial if we're running a script, so make
                // sure to suppress the serial flag in this case.
                serial = serial && (wrkld.getTraceReader() == null);

                int activeTerminals;
                activeTerminals = work.getInt("active_terminals[not(@bench)]", terminals);
                activeTerminals = work.getInt("active_terminals" + pluginTest, activeTerminals);
                // If using serial, we should have only one terminal
                if (serial && activeTerminals != 1) {
                    LOG.warn("Serial ordering is enabled, so # of active terminals is clamped to 1.");
                    activeTerminals = 1;
                }
                if (activeTerminals > terminals) {
                    LOG.error(String.format("Configuration error in work %d: " +
                                            "Number of active terminals is bigger than the total number of terminals",
                              i));
                    System.exit(-1);
                }

                int time = work.getInt("/time", 0);
                int warmup = work.getInt("/warmup", 0);
                timed = (time > 0);
                if (scriptRun) {
                    LOG.info("Running a script; ignoring timer, serial, and weight settings.");
                }
                else if (!timed) {
                    if (serial)
                        LOG.info("Timer disabled for serial run; will execute"
                                 + " all queries exactly once.");
                    else {
                        LOG.fatal("Must provide positive time bound for"
                                  + " non-serial executions. Either provide"
                                  + " a valid time or enable serial mode.");
                        System.exit(-1);
                    }
                }
                else if (serial)
                    LOG.info("Timer enabled for serial run; will run queries"
                             + " serially in a loop until the timer expires.");
                if (warmup < 0) {
                    LOG.fatal("Must provide nonnegative time bound for"
                            + " warmup.");
                    System.exit(-1);
                }

                wrkld.addWork(time,
                              warmup,
                              rate,
                              weight_strings,
                              rateLimited,
                              disabled,
                        serial,
                        timed,
                              activeTerminals,
                              arrival);
              } // FOR

              // CHECKING INPUT PHASES
              int j = 0;
              for (Phase p : wrkld.getAllPhases()) {
                  j++;
                  if (p.getWeightCount() != wrkld.getNumTxnTypes()) {
                      LOG.fatal(String.format("Configuration files is inconsistent, phase %d contains %d weights but you defined %d transaction types",
                                              j, p.getWeightCount(), wrkld.getNumTxnTypes()));
                      if (p.isSerial()) {
                          LOG.fatal("However, note that since this a serial phase, the weights are irrelevant (but still must be included---sorry).");
                      }
                      System.exit(-1);
                  }
              } // FOR

              // Generate the dialect map
              wrkld.init();

              assert (wrkld.getNumTxnTypes() >= 0);
              assert (xmlConfig != null);
            }

            Results r = null;
            SleepingChaperone chaperone = new SleepingChaperone(config);
            System.setProperty("chappie.suffix", Integer.toString(k));
            chaperone = new SleepingChaperone(config);
            try {
                r = runWorkload(benchList, verbose, intervalMonitor);
            } catch (Throwable ex) {
                LOG.error("Unexpected error when running benchmarks.", ex);
                System.exit(1);
            }
            chaperone.cancel();

            for (Thread thread: Thread.getAllStackTraces().keySet()) {
              if (thread.getState() == Thread.State.TERMINATED) {
                thread.join();
              }
            }
          }

        Runtime.getRuntime().halt(0);
    }

    /* buggy piece of shit of Java XPath implementation made me do it
      replaces good old [@bench="{plugin_name}", which doesn't work in Java XPath with lists
    */
    private static List<String> getWeights(String plugin, SubnodeConfiguration work) {
      List<String> weight_strings = new LinkedList<String>();
      @SuppressWarnings("unchecked")
      List<SubnodeConfiguration> weights = work.configurationsAt("weights");
      boolean weights_started = false;

      for (SubnodeConfiguration weight : weights) {
        // stop if second attributed node encountered
        if (weights_started && weight.getRootNode().getAttributeCount() > 0) {
           break;
        }
        // start adding node values, if node with attribute equal to current
        // plugin encountered
        if (weight.getRootNode().getAttributeCount() > 0 && weight.getRootNode().getAttribute(0).getValue().equals(plugin)) {
           weights_started = true;
        }
        if (weights_started) {
           weight_strings.add(weight.getString(""));
        }
      }
      return weight_strings;
    }

    private static Results runWorkload(List<BenchmarkModule> benchList, boolean verbose, int intervalMonitor) throws QueueLimitException, IOException {
        List<Worker<?>> workers = new ArrayList<Worker<?>>();
        List<WorkloadConfiguration> workConfs = new ArrayList<WorkloadConfiguration>();
        for (BenchmarkModule bench : benchList) {
            LOG.info("Creating " + bench.getWorkloadConfiguration().getTerminals() + " virtual terminals...");
            workers.addAll(bench.makeWorkers(verbose));
            // LOG.info("done.");

            int num_phases = bench.getWorkloadConfiguration().getNumberOfPhases();
            LOG.info(String.format("Launching the %s Benchmark with %s Phase%s...",
                    bench.getBenchmarkName().toUpperCase(), num_phases, (num_phases > 1 ? "s" : "")));
            workConfs.add(bench.getWorkloadConfiguration());

        }
        Results r = ThreadBench.runRateLimitedBenchmark(workers, workConfs, intervalMonitor);
        LOG.info(SINGLE_LINE);
        LOG.info("Rate limited reqs/s: " + r);
        return r;
    }

    private static void printUsage(Options options) {
        HelpFormatter hlpfrmt = new HelpFormatter();
        hlpfrmt.printHelp("oltpbenchmark", options);
    }

    /**
     * Returns true if the given key is in the CommandLine object and is set to
     * true.
     *
     * @param argsLine
     * @param key
     * @return
     */
    private static boolean isBooleanOptionSet(CommandLine argsLine, String key) {
        if (argsLine.hasOption(key)) {
            LOG.debug("CommandLine has option '" + key + "'. Checking whether set to true");
            String val = argsLine.getOptionValue(key);
            LOG.debug(String.format("CommandLine %s => %s", key, val));
            return (val != null ? val.equalsIgnoreCase("true") : false);
        }
        return (false);
    }

    public static String getAssertWarning() {
        String msg = "!!! WARNING !!!\n" +
                     "OLTP-Bench is executing with JVM asserts enabled. This will degrade runtime performance.\n" +
                     "You can disable them by setting the config option 'assertions' to FALSE";
        return StringBoxUtil.heavyBox(msg);
    }
}
