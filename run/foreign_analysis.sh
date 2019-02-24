#!/bin/bash

# expected inputs:
# ./foreign_analysis.sh [benchmark top-level directory] [reference top-level directory]

root=$1
reference=$2

for benchmark in $root/*; do
  echo $benchmark
  sudo rm -rf $benchmark/processed
  sudo rm -rf $benchmark/summary
  analysis/processing.py -path $benchmark
  analysis/summarization.py -path $benchmark
  analysis/correlation.py -path $benchmark -reference $reference/${benchmark##*/}
done
