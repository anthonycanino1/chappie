#!/bin/bash

# expected inputs:
# ./chappie_benchmark.sh <iters>

export MODE=SAMPLE

mkdir -p chappie_test

rm -rf chappie_test/$MODE
mkdir chappie_test/$MODE

if [ "$#" -eq  "1" ]
  then
    export iters=$1
  else
    export iters=9
fi

./benchmark.sh "./chappie_test.sh" -i $iters -d chappie_test/$MODE

echo "=================================================="
echo "Processing"
echo "=================================================="
if [ $MODE == NOP ]; then
  sudo rm -rf chappie_test/NOP/processed
  ./analysis/nop_processing.py -path chappie_test/NOP
else
  sudo rm -rf chappie_test/SAMPLE/processed
  sudo rm -rf chappie_test/SAMPLE/summary
  analysis/processing.py -path chappie_test/SAMPLE
  # analysis/summarization.py -path chappie_test/SAMPLE -reference chappie_test/NOP
fi
