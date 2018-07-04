#!/bin/bash

if [ "$#" -eq  "0" ]
  then
    ./run.sh ../test/chappie_test.jar "" chappie_test.Benchmark 5
else
  ./run.sh ../test/chappie_test.jar "" chappie_test.Benchmark $1
fi
