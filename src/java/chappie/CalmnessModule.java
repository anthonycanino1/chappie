package chappie;

import clerk.DataSource;
import clerk.Processor;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;
import java.util.ArrayList;
import java.util.function.Supplier;

/** Module to provide the eflect implementation. */
@Module
interface CalmnessModule {
  @Provides
  @DataSource
  @IntoSet
  static Supplier<?> provideCalmnessSource() {
    return FrequencyHistogram::new;
  }

  @Provides
  static Processor<?, Iterable<FrequencyHistogram>> provideProcessor() {
    return new Processor<FrequencyHistogram, Iterable<FrequencyHistogram>>() {
      private ArrayList<FrequencyHistogram> data = new ArrayList<>();

      @Override
      public void accept(FrequencyHistogram hist) {
        data.add(hist);
      }

      @Override
      public Iterable<FrequencyHistogram> get() {
        ArrayList<FrequencyHistogram> data = this.data;
        this.data = new ArrayList<>();
        return data;
      }
    };
  }
}
