#!/bin/bash

# expected inputs:
# ./chappie_test.sh -d <directory>

directory=chappie_test
case $1 in
  -d) directory=$2;;
esac

export CHAPPIE_DIRECTORY=$directory
./run.sh ../test/chappie_test.jar chappie_test.Benchmark 5
