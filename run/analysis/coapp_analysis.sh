#!/bin/bash

# expected inputs:
# ./coapp_analysis.sh [benchmark top-level directory] [reference top-level directory]

root=$1
reference=$2
method_reference=$3

# dir=`dirname "$0"`
# export CHAPPIE_PATH=$dir/../..

sudo rm -rf $root/summary

for experiment in $root/*; do
  echo $experiment
  bench_names=${experiment##*/}

  bench_name=${bench_names%%_*}
  sudo rm -rf $experiment/benchmark_1/processed
  sudo rm -rf $experiment/benchmark_1/summary
  $CHAPPIE_PATH/run/analysis/processing.py -path $experiment/benchmark_1
  $CHAPPIE_PATH/run/analysis/summarization.py -path $experiment/benchmark_1 -reference $reference/$bench_names/benchmark_1
  $CHAPPIE_PATH/run/analysis/correlation.py -path $experiment/benchmark_1 -reference $method_reference/$bench_name

  bench_name=${bench_names##*_}
  sudo rm -rf $experiment/benchmark_2/processed
  sudo rm -rf $experiment/benchmark_2/summary
  $CHAPPIE_PATH/run/analysis/processing.py -path $experiment/benchmark_2
  $CHAPPIE_PATH/run/analysis/summarization.py -path $experiment/benchmark_2 -reference $reference/$bench_names/benchmark_2
  $CHAPPIE_PATH/run/analysis/correlation.py -path $experiment/benchmark_2 -reference $method_reference/$bench_name

  $CHAPPIE_PATH/run/analysis/coapp_summarization.py -path $experiment
done

$CHAPPIE_PATH/run/analysis/coapp_plotting.py -path $root
