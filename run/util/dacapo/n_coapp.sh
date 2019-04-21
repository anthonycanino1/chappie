#!/bin/bash

# expected inputs:
# ./coapp_callback_benchmark.sh -m <mode>

# benchmarks=(
# "avrora_avrora"
# "jython_jython"
# "avrora_tradebeans"
# "h2_tradebeans"
# "avrora_tradesoap"
# "h2_tradesoap"
# "tradebeans_tradebeans"
# )
benchmarks=(
h2
)

iters=(
5
)

mode=FULL
case $1 in
  -m) mode=$2;;
esac
export MODE=$mode

mkdir -p dacapo
mkdir -p dacapo/reference

if [ $MODE == NOP ]; then
  path=dacapo/reference/coapp
else
  path=dacapo/coapp
fi

mkdir $path

# command=$CHAPPIE_PATH/run/util/dacapo/dacapo.sh
# for benchmark in "${benchmarks[@]}"; do
#   for iter in "${iters[@]}"; do
#     directory=$path/${benchmark}_${iter}
#     $CHAPPIE_PATH/run/execution/n_coapp.sh $command $benchmark $iter -d $directory
#   done
# done

echo "=================================================="
echo "Processing"
echo "=================================================="
if [ $MODE == NOP ]; then
  for benchmark in "${benchmarks[@]}"; do
    for iter in "${iters[@]}"; do
      for i in $(seq 1 ${iter} ); do
        $CHAPPIE_PATH/run/analysis/nop_processing.py -path $path/${benchmark}_${iter}/benchmark_${i}
      done
    done
  done
else
  # echo "not implemented"
  $CHAPPIE_PATH/run/analysis/n_coapp_analysis.sh $path ./dacapo/reference/coapp ./dacapo/dacapo
fi
