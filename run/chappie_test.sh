#!/bin/bash

export ITERS=1
export MODE=3
export POLLING=2
export CORE_RATE=2
export MEMORY=1

if [ "$#" -eq  "0" ]
  then
    ./run.sh ../test/chappie_test.jar "" chappie_test.Benchmark 5
else
  ./run.sh ../test/chappie_test.jar "" chappie_test.Benchmark $1
fi
