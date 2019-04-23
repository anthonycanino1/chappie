#!/bin/bash

# expected inputs:
# ./dacapo.sh [program 1] [program 2] command benchmark iters -d <directory>

echo "=================================================="
echo "Starting coapp with $iters processes"
echo "=================================================="
for iter in $(seq 1 $iters); do
  $command $iter&
  let id_${iter}=$!
done

for i in $(seq 1 $iters); do
  pid=id_${i}
  tail --pid=${!pid} -f /dev/null
done
