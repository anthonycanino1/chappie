# chappie #

`chappie` is a statistical profiler that estimates component and method energy consumption for Java runtimes on linux. `chappie` has only been tested on debian 9.

## building ##

`chappie` requires the following build tools:

 - `java` (works on 8 but is intended for use with 9+)
 - `ant`
 - `maven`
 - `jni`
 - `make`

to build all necessary components, first build the submodules in vendor. Then run:

```bash
ant deps
ant jar
```

`ant` will take care of the rest.

## profiling ##

`chappie` is run asynchronously around a block of code:

```java
import chappie.Chaperone;

public class FooWrapper {
  public static void main() {
    Chaperone chappie = new Chaperone();
    chappie.start();

    // program body

    chappie.stop();
  }
}
```

## running ##

`chappie.sh` mimics `java` calls but also sets up dependencies:

```bash
./chappie.sh -cp foo-wrapper.jar FooWrapper foo_0
```

### chappie args ###
`chappie` accepts the following arguments:
 - `-d/--work-directory`: path where collected data is stored; defaults to `./chappie-logs`
 - `-Dchappie.<arg>`: chappie uses system properties to manually control sampling rates
   - `rate`: base sampling rate in millis; rate of 0 will only collect CPU frequency data at 512ms
   - `vm/os/freq/rapl`: sampling factor for profilers; the profiler will collect data every `-Dchappie.rate` * `-Dchappie.<profiler_rate>` ms
   - `trace`: sampling rate for the async-profiler in ns; the buffers are flushed every 10 epochs, so make sure the async-profiler rate is not too fast relative to the base rate

example:
```bash
# profile FooWrapper at 20ms for all profilers except the os profiler at 80ms
./chappie.sh -d ~/data/foo-experiment -Dchappie.rate=20 -Dchappie.os=4 -cp foo-wrapper.jar FooWrapper foo_0
```
