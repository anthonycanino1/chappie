#!/bin/bash

# export ITERS=1
export MODE=SAMPLE
export POLLING=4
export CORE_RATE=1
export JRAPL_RATE=1
export READ_JIFFIES=true

iter=9
rm -rf chappie.benchmark
mkdir chappie.benchmark

benchmark=chappie_test

export DIRECTORY=${benchmark}
rm -rf $benchmark

mkdir $benchmark
for i in $(seq 0 $iter); do
  echo "Iteration $i/$iter"
  ./chappie_test.sh $benchmark

  # # new signature for run:
  # # ./run.sh <target.jar> <main class to run> <main args>
  # ./run.sh ../test/chappie_test.jar chappie_test.Benchmark 5

  mv $benchmark/chappie.id.csv $benchmark/chappie.id.${i}.csv
  mv $benchmark/chappie.runtime.csv $benchmark/chappie.runtime.${i}.csv
  mv $benchmark/chappie.application.csv $benchmark/chappie.application.${i}.csv
  mv $benchmark/chappie.activeness.csv $benchmark/chappie.activeness.${i}.csv
  mv $benchmark/chappie.thread.csv $benchmark/chappie.thread.${i}.csv
  mv $benchmark/chappie.trace.csv $benchmark/chappie.trace.${i}.csv
  mv $benchmark/chappie.jiffies.csv $benchmark/chappie.jiffies.${i}.csv
  mv log.hpl $benchmark/chappie.stack.${i}.csv

  # for file in $directory/*.*.csv; do
  #   echo $directory/${file}.${i}.csv
  #   mv $directory/$file $directory/${file}.${i}.csv
  # done
  echo ''
done
mv $benchmark chappie.benchmark/$benchmark

./processing.py -path chappie.benchmark/chappie_test
./summarization.py -path chappie.benchmark/chappie_test
# ./plotting.py -path chappie.benchmark
