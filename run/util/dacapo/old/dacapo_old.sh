#!/bin/bash

# expected inputs:
# ./dacapo.sh [benchmark] -d <directory>

benchmark=$1
directory=$benchmark
case $2 in
  -d) directory=$3;;
esac

dir=`dirname "$0"`
export EXTRA_JARS="$dir/../util/chappie_callback.jar"

export CHAPPIE_DIRECTORY=$directory
rm -rf $directory/scratch
./run.sh ../../benchmark_jars/dacapo-9.12-MR1-bach.jar Harness "$benchmark --no-validation --iterations 3 --scratch-directory $directory/scratch --callback test.ChappieCallback"
rm -rf $directory/scratch
