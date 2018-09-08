#!/bin/bash

export ITERS=10
export MODE=FULL_SAMPLE
# export POLLING=1
# export CORE_RATE=1
export MEMORY=true
export STACK_PRINT=true
export INSTRUMENT=true

if [ "$#" -eq  "0" ]
  then
    ./run.sh ../test/chappie_test.jar "" chappie_test.Benchmark 5
else
  ./run.sh ../test/chappie_test.jar "" chappie_test.Benchmark $1
fi
