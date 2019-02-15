#!/bin/bash

# expected inputs:
# ./coapp_benchmark.sh <iters>

rm -rf coapp
mkdir coapp

benchmarks=(
"avrora_avrora"
"batik_batik"
"avrora_batik"
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
  ./benchmark.sh "./coapp.sh \"./dacapo.sh $first\" \"./dacapo.sh $second\"" -i $iters -d coapp
  if [ $first == $second ]
    then
      suffix1=_1
      suffix2=_2
    else
      suffix1=""
      suffix2=""
  fi

  mkdir -p coapp/$benchmark
  mv coapp/benchmark_1 coapp/$benchmark/benchmark_1
  mv coapp/benchmark_2 coapp/$benchmark/benchmark_2
done

echo "=================================================="
echo "Processing"
echo "=================================================="
for benchmark in coapp/*; do
  ./analysis.sh $benchmark
  echo $benchmark
  ./analysis/coapp_summarization.py -path $benchmark
  echo "merged $benchmark"
done
