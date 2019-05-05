#!/bin/bash

# expected inputs:
# ./analysis.sh [benchmark top-level directory] [reference top-level directory]

root=$1
reference=$2

# dir=`dirname "$0"`
# export CHAPPIE_PATH=$dir/../..

sudo rm -rf $root/plots

echo $root/*

for benchmark in $root/*; do
  echo $benchmark
  sudo rm -rf $benchmark/processed
  sudo rm -rf $benchmark/summary
  python3 $CHAPPIE_PATH/run/analysis/processing.py -path $benchmark
  # exit
  python3 $CHAPPIE_PATH/run/analysis/summarization.py -path $benchmark -reference $reference/${benchmark##*/}
  # /home/rsaxena3/work/chappie/run/pagerank_vm2_hp2_os40/graphchi/reference/graphchi/Pagerank
done

$CHAPPIE_PATH/run/analysis/plotting.py -path $root
