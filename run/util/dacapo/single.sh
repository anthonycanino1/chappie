#!/bin/bash

# expected inputs:
# ./dacapo_callback_benchmark.sh -m <mode>

# export CHAPPIE_PATH=/home/timur/Projects/chappie
mkdir -p dacapo
mkdir -p dacapo/reference

if [ $MODE == NOP ]; then
  path=dacapo/reference/dacapo
else
  path=dacapo/dacapo
fi

mkdir -p $path
$CHAPPIE_PATH/run/util/dacapo/dacapo.sh $benchmark -d $path/$benchmark
sudo rm -rf $path/$benchmark/processed
$CHAPPIE_PATH/run/analysis/nop_processing.py -path $path/$benchmark
