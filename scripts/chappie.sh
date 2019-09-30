#!/bin/bash

chappie_root=$(realpath `dirname "$0"`/..)

# python3 $chappie_root/scripts/run "$@"
$(python3 $chappie_root/scripts/run "$@")
python3 $chappie_root/scripts/analysis "$@"
