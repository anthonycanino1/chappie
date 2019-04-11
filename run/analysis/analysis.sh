#!/bin/bash

# expected inputs:
# ./analysis.sh [benchmark top-level directory] [reference top-level directory]

root=$1
reference=$2

# dir=`dirname "$0"`
# export CHAPPIE_PATH=$dir/../..

for benchmark in $root/*; do
  echo $benchmark
  sudo rm -rf $benchmark/processed
  sudo rm -rf $benchmark/summary
  $CHAPPIE_PATH/run/analysis/processing.py -path $benchmark
  $CHAPPIE_PATH/run/analysis/summarization.py -path $benchmark -reference $reference/${benchmark##*/}
done

$CHAPPIE_PATH/run/analysis/plotting.py -path $root