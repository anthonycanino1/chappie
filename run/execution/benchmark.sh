#!/bin/bash

# expected inputs:
# ./benchmark.sh [program call] -i <iters> -d <directory>

iters=9
case $2 in
  -i) iters=$3;;
  -d) directory=$3;;
esac

directory=data
case $4 in
  -i) iters=$5;;
  -d) directory=$5;;
esac

mkdir -p $directory

for i in $(seq 0 $iters); do
  echo "=================================================="
  echo "Iteration $((i+1))/$((iters+1))"
  echo "=================================================="

  export CHAPPIE_SUFFIX=$i
  eval "$1 -d $directory"
done
