# chappie #

`chappie` is a runtime observer for Java that collects data for the functional state of an executed program at the thread level. Collected data spans the thread, application, and system levels, indexed by an integer epoch.

## Utilities ##

`chappie` uses the following libraries for sampling:

#### [jRAPL](http://kliu20.github.io/jRAPL) ####
Java wrapper for Intel's [RAPL](https://en.wikipedia.org/w/index.php?title=Running_average_power_limit&redirect=yes) interface used to make per socket energy measurements.

#### [GLIBC](https://www.gnu.org/software/libc/) ####
Library tools for Linux systems. We use the syscall interface to get process and thread ids at runtime.

#### [javassist](http://www.javassist.org/) ####
Java bytecode manipulation library. **javassist** allows for runtime instrumentation of java classes. We instrument all classes that implement [java.lang.Runnable](https://docs.oracle.com/javase/8/docs/api/java/lang/Runnable.html) to add their thread id to an internal map before calling `run()`.

#### [Honest Profiler](https://github.com/jvm-profiling-tools/honest-profiler) ####
A Java profiler that reduces the overhead incurred from traditional profilers by leveraging the JVM. It implements asynchronous stack fetching for threads to remove the inherent overhead from safepoints that are required for [getStackTrace()](https://docs.oracle.com/javase/8/docs/api/java/lang/Thread.html).

## Building ##

`$ ant jar` at the top level builds `chappie.jar`, which contains all the necessary requirements to run chappie around a program.

## Running ##

GLIBC is required to run chappie. This can be installed with a package manager or built from source.

`chappie` initializes an observer, bootstraps the program, and performs clean up. Command line arguments are used to direct chappie but it is recommended instead to use `run/run.sh`. The call signature for `run/run.sh` is:

run/run.sh <jar_path> <extra_jars> <jar_main_class> <args>

In addition, a small testing program is provided at `test`, called `chappie_test`. It performs traditional matrix multiplication. `chappie_test.jar` can be built with `$ ant jar` in `test`. A bootstrapping script for `chappie_test` is provided at `run/chappie_test.sh` that can also be used as a model for implementing bootstrapping scripts for other programs.

## Output ##

When the program terminates, chappie writes a set of output files to `chappie.<jar name>`. Included are:

 - `chappie.thread.csv`: Data for threads at the Java-level
 - `chappie.trace.csv`: Data for socket energy consumption at the Java-level
 - `log.hpl`: Data for thread stack traces at the Java-level
 - `chappie.application.csv`: Data for threads at the OS-level
 - `chappie.jiffies.csv`: Data for cpu jiffies consumption at the OS-level
