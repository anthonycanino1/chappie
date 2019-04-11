#!/bin/bash

# expected inputs:
# ./foreign_analysis.sh [benchmark top-level directory] [reference top-level directory]

root=$1
reference=$2
method_reference=$3

for benchmark in $root/*; do
  echo $benchmark
  sudo rm -rf $benchmark/processed
  sudo rm -rf $benchmark/summary
  $CHAPPIE_PATH/run/analysis/processing.py -path $benchmark
  $CHAPPIE_PATH/run/analysis/summarization.py -path $benchmark -reference $reference/${benchmark##*/}
  $CHAPPIE_PATH/run/analysis/correlation.py -path $benchmark -reference $method_reference/${benchmark##*/}
done

$CHAPPIE_PATH/run/analysis/foreign_plotting.py -path $path
