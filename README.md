# chappie #

An implementation of a **chaperone** monitor for multi-threaded Java runtimes.

### Chaperone ###

A **chaperone** refers to an agent that has privileged access to another entity. This implementation provides a global observer that roughly characterizes a functional thread state during runtime. The functional thread state is *currently* composed of activity, executing core, energy consumed, and memory allocated. More details regarding the thread state can be found in the [wiki](https://github.com/anthonycanino1/chappie/wiki/Thread-State).

#### Building ####

chappie makes energy measurements using [jRAPL](http://kliu20.github.io/jRAPL). This needs to be built before building chappie.jar. `$ make` in `vendor/jrapl-port` will build jRAPL for the target system.

`$ ant jar` at the top level will build `chappie.jar`.

#### Running ####

chappie monitors another program's runtime by using a bootstrapper. *Currently*, command line arguments are provided to direct chappie:

```bash
export $CHAPPIE_PATH=<location of chappie.jar>
export $JARS="$CHAPPIE_PATH:<program jar>"
jar=<url path to program jar>

java -cp $JARS -javaagent:$CHAPPIE_PATH chappie.Chaperone $jar <jar main> <args...>
```

More details regarding execution can be found in the [wiki](https://github.com/anthonycanino1/chappie/wiki/Benchmarks).

### Results ###

At program termination, the chaperone writes all observations to two `.csv` files. `chappie.trace.csv` contains a long-format time series of the number of threads for each Java activity state. `chappie.thread.csv` contains a long-format time-series of differential package and DRAM energy consumption and differential memory consumption for each thread. These are formatted for ease of use in R or csv manipulation, such as `pandas`. More details regarding results can be found in the [wiki](https://github.com/anthonycanino1/chappie/wiki/Scripts-and-Figure-Generation).
