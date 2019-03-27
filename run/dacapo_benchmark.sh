#!/bin/bash

# expected inputs:
# ./dacapo_benchmark.sh <iters>

export MODE=FULL

mkdir -p dacapo
mkdir -p dacapo/reference

path=dacapo/dacapo

rm -rf $path
mkdir $path

benchmarks=(
avrora
batik
eclipse
fop
h2
jython
luindex
lusearch-fix
pmd
sunflow
tradebeans
tradesoap
xalan
)

if [ "$#" -eq  "1" ]
  then
    export iters=$1
  else
    export iters=9
fi

for benchmark in "${benchmarks[@]}"; do
  ./benchmark.sh "./dacapo.sh $benchmark" -i $iters -d $path/$benchmark
done

echo "=================================================="
echo "Processing"
echo "=================================================="
if [ $MODE == NOP ]; then
  for benchmark in "${benchmarks[@]}"; do
    ./analysis/nop_processing.py -path $path/$benchmark
  done
else
  ./analysis.sh $path dacapo/reference/dacapo
fi
