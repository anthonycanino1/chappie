#!/bin/bash

# expected inputs:
# ./coapp_callback_benchmark.sh -m <mode>

benchmarks=(
"h2_oltp"
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

command1=$CHAPPIE_PATH/run/util/dacapo/dacapo.sh
command2=$CHAPPIE_PATH/run/util/oltp/oltp.sh

for benchmark in "${benchmarks[@]}"; do
  first="${benchmark%%_*}"; second="${benchmark#*_}"
  $CHAPPIE_PATH/run/execution/coapp.sh "$command $first" "$command $second" -d $path

  mkdir -p $path/$benchmark
  mv $path/benchmark_1 $path/$benchmark/benchmark_1
  mv $path/benchmark_2 $path/$benchmark/benchmark_2
done

echo "=================================================="
echo "Processing"
echo "=================================================="
if [ $MODE == NOP ]; then
  for benchmark in "${benchmarks[@]}"; do
    $CHAPPIE_PATH/run/analysis/nop_processing.py -path $path/$benchmark/benchmark_1
    $CHAPPIE_PATH/run/analysis/nop_processing.py -path $path/$benchmark/benchmark_2
  done
else
  $CHAPPIE_PATH/run/analysis/coapp_analysis.sh $path ./dacapo/reference/coapp ./dacapo/dacapo
fi
