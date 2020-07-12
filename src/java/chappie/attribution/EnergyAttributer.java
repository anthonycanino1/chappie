package chappie.attribution;

import chappie.profiling.SampleProcessor;

public interface EnergyAttributer<E extends EnergyAttribution> extends SampleProcessor<Iterable<E>> { }
