# chappie #

An implementation of a **chaperone** monitor for multi-threaded Java systems.

### The chaperone ###

The **chaperone** refers to any agent that has privileged access to another entity. This particular chaperone roughly characterizes functional threads state during executing of a Java program at the application level. This implementation uses a global observer which examines thread state periodically. Thread state is *currently* composed of activity, energy consumption, and memory allocated.

At program termination, the chaperone writes all observations to two `csv` files. `chappie.trace.csv` contains a long-format time series of the number of threads for each Java activity state. `chappie.thread.csv` contains a long-format time-series of differential package and DRAM energy consumption and differential memory consumption for each thread. These are formatted for ease of plotting in R or usage in csv manipulation, such as `pandas`.

#### Usage ####

A single chaperone is currently the correct approach. The following is a sufficient example:

```java
public class ChaperoneTest {
  public static Chaperone chaperone = new GlobalChaperone();

public static void main(String[] args) {
  chaperone.assign();
  // Program execution
  chaperone.dismiss();
}
```

chappie will observe and characterize the threads during the program execution block and archive the results.


#### Building ####

chappie makes energy measurements using jRAPL (<http://kliu20.github.io/jRAPL>). This needs to be built before building chappie.jar. `$ make` in `vendor/jrapl-port` will build jRAPL for the target system.

`$ ant jar` will build a jar file containing the GlobalChaperone for thread characterization.
