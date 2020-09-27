package chappie;

import clerk.DataSource;
import clerk.Processor;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;
import java.util.ArrayList;
import java.util.function.Supplier;

/** Module to provide the eflect implementation. */
@Module
interface AlignmentModule {
  @Provides
  @DataSource
  @IntoSet
  static Supplier<?> provideTraceSource() {
    return () -> null; // FrequencyHistogram::new;
  }

  @Binds
  abstract Processor<?, Iterable<StackTraceRanking>> provideProcessor(StackTraceAligner aligner);
}
