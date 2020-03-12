#!/bin/bash
chappie_root=$(realpath `dirname "$0"`)/../../..

# guard arguments in case i screw up
work_dir=./chappie-logs

rate=1

bench=graphchi
size=default
iters=5

# real args
work_dir=${chappie_root}/chappie-logs
rm -rf $work_dir/*
mkdir $work_dir

rate=16

$(realpath `dirname "$0"`)/dacapo.sh $work_dir "-Dchappie.rate=$rate" $bench "--size $size --iterations $iters"
python3 ${chappie_root}/src/python/analysis -d $work_dir
