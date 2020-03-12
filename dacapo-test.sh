#!/bin/bash
chappie_root=$(realpath `dirname "$0"`)

# guard arguments in case i screw up
work_dir=./chappie-logs

vm=1
os=4
trace=1000000

bench=jme
size=default
iters=10

# real args
work_dir=./chappie-logs
rm -rf $work_dir/*

./dacapo.sh $work_dir "-Dchappie.rate=0" $bench "--size $size --iterations $iters"
python3 src/python/analysis -d $work_dir
