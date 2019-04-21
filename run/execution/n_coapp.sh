#!/bin/bash

# expected inputs:
# ./dacapo.sh [program 1] [program 2] command benchmark iters -d <directory>

command=$1
benchmark=$2
iters=$3
directory='.'
case $4 in
  -d) directory=$5
esac

echo "=================================================="
echo "Starting coapp with $iters processes"
echo "=================================================="
for iter in $(seq 1 $iters); do
  echo $directory
  $command $benchmark -d $directory/benchmark_${iter} &
  let id_${iter}=$!
done

for i in $(seq 1 $iters); do
  pid=id_${i}
  tail --pid=${!pid} -f /dev/null
done
