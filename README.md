# chappie

An implementation of a **Chaperone** monitor for multi-threaded Java systems.

## Chaperone

The **Chaperone**:
```
Provide an agent that is given privileged access to an entity.
```

The Chaperone monitor is implemented to characterize threads state during executing of a Java program.

There are two provided implementations:

1) GlobalChaperone - A single thread that collects all desired data.

2) PeepholeChaperone - A single thread that collects data as long as at least one request has been made to it. When no one is any longer requesting to be chaperoned, it does not measure anything.

These implementations archive time indexed tuples for: the sets of active and inactive threads; the power readings of each thread; and the core a thread is operating on.

To use the classes implemented here, a single instance of the Chaperone is the best approach. However, a Chaperone could be implemented as a field of a class. For simplicity, the following is a sufficient example:

```
public class Utilities {
  public static Chaperone chaperone = new PeepholeChaperone();
}
```

To request a Chaperone, use the following:

```
Utilities.chaperone.assign();
// Code that needs to be monitored
Utilities.chaperone.dismiss();
```

To archive the results, use the following:

```
//Program execution
Utilities.chaperone.retire();
```

## Building

**$ ant jar** will build a jar file containing both the Global and Peephole implementations of the Chaperone.
