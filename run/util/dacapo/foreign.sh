#!/bin/bash

# expected inputs:
# ./foreign_benchmark.sh -m <mode>

benchmarks=(
# avrora
# batik
# eclipse
# fop
h2
# jython
# luindex
# lusearch-fix
# pmd
# sunflow
# tradebeans
# tradesoap
# xalan
)

export PARSECBIN=/david.cs.binghamton.edu/home/acanino1/Projects/parsec-3.0/bin

mode=FULL
case $1 in
  -m) mode=$2;;
esac
export MODE=$mode

mkdir -p dacapo
mkdir -p dacapo/reference

if [ $MODE == NOP ]; then
  path=dacapo/reference/foreign
else
  path=dacapo/foreign
fi

mkdir $path

command=$CHAPPIE_PATH/run/util/dacapo/dacapo.sh

for benchmark in "${benchmarks[@]}"; do
  $CHAPPIE_PATH/run/execution/foreign.sh "$command $benchmark" "$PARSECBIN/parsecmgmt -a run -p ferret -i simlarge" -d $path/$benchmark
  rm -rf $directory/scratch
done

echo "=================================================="
echo "Processing"
echo "=================================================="
if [ $MODE == NOP ]; then
  for benchmark in "${benchmarks[@]}"; do
    $CHAPPIE_PATH/run/analysis/nop_processing.py -path $path/$benchmark
  done
else
  $CHAPPIE_PATH/run/analysis/foreign_analysis.sh $path ./dacapo/reference/foreign ./dacapo/dacapo
fi
