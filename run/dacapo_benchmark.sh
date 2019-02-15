#!/bin/bash

# expected inputs:
# ./dacapo_benchmark.sh <iters>

rm -rf dacapo
mkdir dacapo

benchmarks=(
avrora
batik
eclipse
fop
h2
jython
luindex
lusearch
pmd
sunflow
tomcat
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
  ./benchmark.sh "./dacapo.sh $benchmark" -i $iters -d dacapo/$benchmark
done

echo "=================================================="
echo "Processing"
echo "=================================================="
./analysis.sh dacapo
