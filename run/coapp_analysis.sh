#!/bin/bash

# expected inputs:
# ./coapp_analysis.sh [benchmark top-level directory] [reference top-level directory]

root=$1
reference=$2

for experiment in $root/*; do
  echo $experiment
  bench_names=${experiment##*/}

  bench_name=${bench_names%%_*}
  sudo rm -rf $experiment/benchmark_1/processed
  sudo rm -rf $experiment/benchmark_1/summary
  analysis/processing.py -path $experiment/benchmark_1
  analysis/summarization.py -path $experiment/benchmark_1
  analysis/correlation.py -path $experiment/benchmark_1 -reference $reference/$bench_name

  bench_name=${bench_names##*_}
  sudo rm -rf $experiment/benchmark_2/processed
  sudo rm -rf $experiment/benchmark_2/summary
  analysis/processing.py -path $experiment/benchmark_2
  analysis/summarization.py -path $experiment/benchmark_2
  analysis/correlation.py -path $experiment/benchmark_2 -reference $reference/$bench_name

  analysis/coapp_summarization.py -path $experiment
done
