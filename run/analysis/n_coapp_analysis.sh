#!/bin/bash

# expected inputs:
# ./coapp_analysis.sh [benchmark top-level directory] [reference top-level directory]

root=$1
reference=$2
method_reference=$3
iters=$4

# dir=`dirname "$0"`
# export CHAPPIE_PATH=$dir/../..

sudo rm -rf $root/summary
sudo rm -rf $root/plots

# echo $root

for experiment in $root/*; do
  # # echo $experiment
  # for benchmark in $experiment/*; do
  #   echo $benchmark
  #   sudo rm -rf $benchmark/summary
  #
  #   name=${benchmark#*/}
  #   name=${name#*/}
  #   name=${name%%/*}
  #   name=${name%%/*}
  #   expr=$name
  #
  #   name=${name%%_*}
  #   echo $name
  #
  #   bench=${benchmark##*/}
  #   echo $bench
  #
  #   sudo rm -rf $benchmark/processed
  #   sudo rm -rf $benchmark/summary
  #   $CHAPPIE_PATH/run/analysis/processing.py -path $benchmark
  #   $CHAPPIE_PATH/run/analysis/summarization.py -path $benchmark -reference $reference/$expr/$bench
  #   $CHAPPIE_PATH/run/analysis/correlation.py -path $benchmark -reference $method_reference/$name
  # done

  $CHAPPIE_PATH/run/analysis/coapp_summarization.py -path $experiment
  done

$CHAPPIE_PATH/run/analysis/coapp_plotting.py -path $root
