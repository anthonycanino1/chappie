#!/bin/bash

chappie_root=$(realpath `dirname "$0"`/..)

# python3 $chappie_root/scripts/run "$@"
# $(python3 $chappie_root/scripts/run "$@")
# tail -n 50 ../chappie-data/test/raw/method.csv
python3 $chappie_root/scripts/analysis "$@"

# benchs=(
# "biojava"
# # "fop"
# # "h2o"
# # "jme"
# # "jython"
# # "kafka"
# # "xalan"
# # "zxing"
# # "avrora"
# # "eclipse"
# # "pmd"
# # "sunflow"
# # "graphchi"
# )
#
# for bench in "${benchs[@]}"; do
#   echo $bench
#   for case in /home/timur/projects/chappie-data/baseline/$bench/*; do
#     echo $case
#     python3 $chappie_root/scripts/analysis -dir $case &
#   done
#
#   tail --pid=$! -f /dev/null
# done
#
#
# # for i in /home/timur/projects/chappie-data/batik/*; do
# #   echo $i
# #   python3 $chappie_root/scripts/analysis -dir $i &
# # done
