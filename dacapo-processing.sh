#!/bin/bash

chappie_root=$(realpath `dirname "$0"`)

benchs=(batik biojava eclipse fop graphchi h2 jme jython kafka kafka luindex lusearch pmd sunflow tomcat xalan)


for bench in /home/timur/projects/pldi-2020/baseline/*; do
  work_dir=${chappie_root}/../chappie-data/nop/${bench}
  python3 ${chappie_root}/scripts/analysis -d $work_dir &

  bench=${chappie_root}/../chappie-data/baseline/${bench}
  for case in $bench/*; do
    echo $case
    python3 ${chappie_root}/scripts/analysis -d $case &
  done

  wait $!
  # exit
done

# wait $!
