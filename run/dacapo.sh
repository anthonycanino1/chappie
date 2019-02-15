#!/bin/bash

# expected inputs:
# ./dacapo.sh [benchmark] -d <directory>

benchmark=$1
directory=$benchmark
case $2 in
  -d) directory=$3;;
esac

export CHAPPIE_DIRECTORY=$directory
rm -rf $directory/scratch
./run.sh ../../benchmark_jars/dacapo-9.12-bach.jar Harness "-no-validation --scratch-directory $directory/scratch $benchmark"
rm -rf $directory/scratch
