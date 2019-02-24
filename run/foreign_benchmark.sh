#!/bin/bash

# expected inputs:
# ./foreign_benchmark.sh <iters>

# rm -rf $directory/scratch

# rm -rf dacapo_foreign
# mkdir dacapo_foreign

benchmarks=(
avrora
batik
eclipse
# fop
h2
# jython
# luindex
# lusearch
# pmd
sunflow
# tomcat
# tradebeans
# tradesoap
# xalan
)

export PARSECBIN=/home/acanino1/Projects/parsec-3.0/bin

iters=9

if [ "$#" -eq  "1" ]
  then
    iters=$1
  else
    iters=9
fi

for benchmark in "${benchmarks[@]}"; do
  ./benchmark.sh "./foreign.sh \"./dacapo.sh $benchmark\" \"$PARSECBIN/parsecmgmt -a run -p ferret -i simlarge\"" -i $iters -d dacapo_foreign/$benchmark
  rm -rf $directory/scratch
done

echo "=================================================="
echo "Processing"
echo "=================================================="
./foreign_analysis.sh dacapo_foreign data/dacapo
