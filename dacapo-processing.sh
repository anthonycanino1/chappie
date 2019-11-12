#!/bin/bash

chappie_root=$(realpath `dirname "$0"`)

benchs=(luindex tomcat)

for bench in ${benchs[@]}; do
  bench=${chappie_root}/../chappie-data/baseline/${bench}
  for case in $bench/*; do
    echo $case
    python3 ${chappie_root}/scripts/analysis -d $case &
  done
done

wait $!
