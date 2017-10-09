# chappie

An implementation of the **Chaperone** pattern design for monitoring thread activity during program run time.

## Chaperone

The **Chaperone** Pattern:
*```Provide an agent that is given permission to access an entity for a span of time.
```*

The Chaperone pattern here is implemented to monitoring the number of threads in the "RUNNABLE" state during executing of a Java program.

There are two provided implementations:

1) AsynchronousChaperone - A single thread runs in the background and is active as long as at least one request has been made to it. When no one is any longer requesting to be chaperoned, it does not log threads.

2) PoolingChaperone - A single thread acts as a pool and assigns threads to monitor thread activity until the requesting entity dismisses them.

Both of these implementations store a set of Tuples containing a time stamp and a set of threads that were active during that time stamp that is stored to "chappie.log" at the end of execution.

To use the classes implemented here, a singleton instance of the Chaperone is the best approach currently. However, a Chaperone could be implemented as a field of a class. For simplicity, the following is a sufficient example:

```
public class Utilities {
  public static Chaperone chaperone = new AsynchronousChaperone();
}
```

To request a Chaperone, use the following:

```
Utilities.chaperone.assign();
// Code that needs to be monitored
Utilities.chaperone.dismiss();
```

To store the log to a file, use the following:

```
//Program execution
Utilities.chaperone.retire();
```

## Building

**"$ ant jar"** will build a jar file containing both the Asynchronous and Pooling implementations of the Chaperone.
