#!/bin/bash

# expected inputs:
# ./dacapo_callback_benchmark.sh -m <mode>

# export CHAPPIE_PATH=/home/timur/Projects/chappie

benchmarks=(
Pagerank
# ConnectedComponents
# ALSMatrixFactorization
)

# mode=FULL
# case $1 in
#   -m) mode=$2;;
# esac
# export MODE=$mode

mkdir -p graphchi
mkdir -p graphchi/reference

if [ $MODE == NOP ]; then
  path=graphchi/reference/graphchi
else
  path=graphchi/graphchi
fi

mkdir -p $path

for benchmark in "${benchmarks[@]}"; do
  mkdir $path/$benchmark
  for i in $(seq 0 9); do
    echo "=================================================="
    echo "Iteration $((i+1))/10"
    echo "=================================================="
    export CHAPPIE_SUFFIX=$i
    $CHAPPIE_PATH/run/util/graphchi/graphchi.sh $benchmark -d $path/$benchmark
  done
done

echo "=================================================="
echo "Processing"
echo "=================================================="
if [ $MODE == NOP ]; then
  for benchmark in "${benchmarks[@]}"; do
    echo $benchmark
    sudo rm -rf $path/$benchmark/processed
    $CHAPPIE_PATH/run/analysis/nop_processing.py -path $path/$benchmark
  done
else
  $CHAPPIE_PATH/run/analysis/analysis.sh $path graphchi/reference/graphchi
fi
