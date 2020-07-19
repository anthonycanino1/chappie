package chappie.attribution;

import chappie.profiling.Profile;
import chappie.profiling.SampleProcessor;

/**
 * Interface for a processor that produces iterables of energy profiles.
 * this is not strictly enforced right now.
 */
public interface EnergyAttributer extends SampleProcessor<Iterable<Profile>> { }
