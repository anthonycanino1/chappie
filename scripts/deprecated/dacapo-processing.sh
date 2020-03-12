#!/bin/bash
chappie_root=$(realpath `dirname "$0"`)/../../..
data_root=/home/timur/projects/chappie-data

# guard arguments in case i screw up
work_dir=./chappie-logs

vm=1
os=4
trace=1000000

bench=graphchi
size=default
iters=10

# real args
current_data_root=$data_root/fse2020/baseline

i=0
for bench_dir in $current_data_root/*; do
  # for case_dir in $bench_dir/*; do
    for work_dir in $bench_dir/*; do
      echo $work_dir
      python3 src/python/analysis -d $work_dir &
    done
  # done
done
