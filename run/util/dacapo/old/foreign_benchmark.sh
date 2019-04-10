#!/bin/bash

# expected inputs:
# ./foreign_benchmark.sh <iters>

export MODE=FULL

# mkdir -p

path=dacapo/foreign

rm -rf $path
mkdir $path

benchmarks=(
avrora
# batik
# eclipse
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

export PARSECBIN=/david.cs.binghamton.edu/home/acanino1/Projects/parsec-3.0/bin

iters=9

if [ "$#" -eq  "1" ]
  then
    iters=$1
  else
    iters=9
fi

for benchmark in "${benchmarks[@]}"; do
  ./benchmark.sh "./foreign.sh \"./dacapo.sh $benchmark\" \"$PARSECBIN/parsecmgmt -a run -p ferret -i simlarge\"" -i $iters -d $path/$benchmark
  rm -rf $directory/scratch
done

echo "=================================================="
echo "Processing"
echo "=================================================="
if [ $MODE == NOP ]; then
  for benchmark in "${benchmarks[@]}"; do
    ./analysis/nop_processing.py -path $path/$benchmark
  done
else
  ./foreign_analysis.sh $path ./dacapo/reference/foreign ./dacapo/dacapo
  ./analysis/plotting.sh $path
fi
