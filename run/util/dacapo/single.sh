#!/bin/bash

# expected inputs:
# ./dacapo_callback_benchmark.sh -m <mode>

# export CHAPPIE_PATH=/home/timur/Projects/chappie

benchmarks=(
# avrora
# batik
# eclipse
# fop
h2
# jython
# luindex
# lusearch-fix
# pmd
# sunflow
# tradebeans
# tradesoap
# xalan
)

mode=FULL
export MODE=$mode

mkdir -p dacapo
mkdir -p dacapo/reference

if [ $MODE == NOP ]; then
  path=dacapo/reference/dacapo
else
  path=dacapo/dacapo
fi

mkdir -p $path

for benchmark in "${benchmarks[@]}"; do
  $CHAPPIE_PATH/run/util/dacapo/dacapo.sh $benchmark -d $path/$benchmark
done
