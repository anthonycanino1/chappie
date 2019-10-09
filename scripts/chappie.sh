#!/bin/bash

chappie_root=$(realpath `dirname "$0"`/..)

# python3 $chappie_root/scripts/run "$@"
$(python3 $chappie_root/scripts/run "$@")
# tail -n 50 ../chappie-data/test/raw/method.csv
# python3 $chappie_root/scripts/analysis "$@"
