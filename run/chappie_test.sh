#!/bin/bash

export ITERS=2
export MODE=VM_SAMPLE
export POLLING=4
export CORE_RATE=4
export MEMORY=true
export INSTRUMENT=false
export STACK_PRINT=true

if [ "$#" -eq  "0" ]
  then
    ./run.sh ../test/chappie_test.jar "" chappie_test.Benchmark 5
else
  ./run.sh ../test/chappie_test.jar "" chappie_test.Benchmark $1
fi
