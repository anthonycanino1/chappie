#!/bin/bash

# expected inputs:
# ./chappie experiment.json

experiment=$1
rm -rf sunflow

path=$(python3 ./parser/parse_experiment.py -experiment $experiment)
benchmark="${path}/benchmark/benchmark.json"

for config in $path/config/*; do
  echo $config
  cmd=$(python3 ./parser/parse_case.py -benchmark $benchmark -config $config 2>&1)
  $cmd
done

# processing
python3 ./analysis/nop_processing.py -benchmark $benchmark -config $path/config/NOP.xml
for config in $path/config/*; do
  if [ $config != $path/config/NOP.xml ]; then
    echo $config
    python3 ./analysis/processing.py -benchmark $benchmark -config $config
    python3 ./analysis/summarization.py -benchmark $benchmark -config $config -reference $path/config/NOP.xml
  fi
done

python3 ./analysis/heatmap.py -config $path/config
