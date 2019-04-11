#!/bin/bash

# expected inputs:
# ./dacapo.sh [program 1] [program 2] -d <directory>

directory='.'
case $3 in
  -d) directory=$4
esac

$1 -d $directory/benchmark_1 &
first_id=$!

$2 -d $directory/benchmark_2 &
second_id=$!

tail --pid=${first_id} -f /dev/null
tail --pid=${second_id} -f /dev/null
