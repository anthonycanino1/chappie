#!/bin/bash

rm chappie.chappie_test -rf

export ITERS=1
export MODE=VM_SAMPLE
export POLLING=4
export CORE_RATE=10
export JRAPL_RATE=2
export READ_JIFFIES=true

if [ "$#" -eq  "0" ]
  then
    ./run.sh ../test/chappie_test.jar "" chappie_test.Benchmark 5
  else
    ./run.sh ../test/chappie_test.jar "" chappie_test.Benchmark $1
  fi
