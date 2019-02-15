#!/bin/bash

# expected inputs:
# ./analysis.sh [benchmark top-level directory]

root=$1

for benchmark in $root/*; do
  echo $benchmark
  sudo rm -rf $benchmark/processed
  sudo rm -rf $benchmark/summary
  analysis/processing.py -path $benchmark
  analysis/summarization.py -path $benchmark
done
