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
current_data_root=$data_root/2020/nop
mkdir $current_data_root

work_dir=${current_data_root}/${bench}
sudo rm -rf $work_dir
./dacapo.sh $work_dir "-Dchappie.rate=0" $bench "--size $size --iterations $iters"

current_data_root=$data_root/2020/baseline
mkdir $current_data_root

bench=avrora
size=large
iters=10

mkdir ${current_data_root}/${bench}
for i in `seq 0 3`; do
  work_dir=${current_data_root}/${bench}/${i}
  sudo rm -rf $work_dir
  ./dacapo.sh $work_dir "-Dchappie.vm=$vm -Dchappie.os=$os -Dchappie.trace=$trace" $bench "--size $size --iterations $iters"
done

current_data_root=$data_root/2020/precision
mkdir $current_data_root

bench=avrora
size=large
iters=10

rate=2
os=2

mkdir ${current_data_root}/${bench}
mkdir ${current_data_root}/${bench}/2-4
for i in `seq 0 3`; do
  work_dir=${current_data_root}/${bench}/2-4/${i}
  sudo rm -rf $work_dir
  ./dacapo.sh $work_dir "-Dchappie.rate=$rate -Dchappie.os=$os -Dchappie.trace=$trace" $bench "--size $size --iterations $iters"
done

rate=4
os=1

mkdir ${current_data_root}/${bench}
mkdir ${current_data_root}/${bench}/2-4
for i in `seq 0 3`; do
  work_dir=${current_data_root}/${bench}/2-4/${i}
  sudo rm -rf $work_dir
  ./dacapo.sh $work_dir "-Dchappie.rate=$rate -Dchappie.os=$os -Dchappie.trace=$trace" $bench "--size $size --iterations $iters"
done
