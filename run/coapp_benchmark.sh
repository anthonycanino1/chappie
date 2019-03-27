#!/bin/bash

# expected inputs:
# ./coapp_benchmark.sh <iters>

export MODE=FULL

mkdir -p COAPP

path=./dacapo/coapp

# rm -rf $path
# mkdir $path

benchmarks=(
# "avrora_avrora"
# "batik_batik"
# "h2_h2"
# "jython_jython"
# "sunflow_sunflow"
# "avrora_batik"
# "avrora_jython"
"h2_jython"
"h2_sunflow"
)

iters=9

if [ "$#" -eq  "1" ]
  then
    iters=$1
  else
    iters=9
fi

for benchmark in "${benchmarks[@]}"; do
  first="${benchmark%%_*}"; second="${benchmark#*_}"
  ./benchmark.sh "./coapp.sh \"./dacapo.sh $first\" \"./dacapo.sh $second\"" -i $iters -d $path

  mkdir -p $path/$benchmark
  mv $path/benchmark_1 $path/$benchmark/benchmark_1
  mv $path/benchmark_2 $path/$benchmark/benchmark_2
done

echo "=================================================="
echo "Processing"
echo "=================================================="
if [ $MODE == NOP ]; then
  for benchmark in "${benchmarks[@]}"; do
    ./analysis/nop_processing.py -path $path/$benchmark/benchmark_1
    ./analysis/nop_processing.py -path $path/$benchmark/benchmark_2
  done
else
  ./coapp_analysis.sh $path ./dacapo/reference/coapp ./dacapo/reference/dacapo
fi
