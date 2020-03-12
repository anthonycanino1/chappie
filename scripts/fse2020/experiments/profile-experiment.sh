#!/bin/bash
chappie_root=$(realpath `dirname "$0"`)/../../..
# this should be changed to the location desired
data_root=/home/timur/projects/chappie-data
mkdir $data_root

data_root=$data_root/fse2020
mkdir $data_root

current_data_root=$data_root/profile
mkdir $current_data_root

benchs=(
  biojava
  jython
  xalan
)
size=default
iters=1

for bench in ${benchs[@]}; do
  mkdir ${current_data_root}/${bench}
  for i in `seq 0 3`; do
    work_dir=${current_data_root}/${bench}/${i}
    sudo rm -rf $work_dir
    $(realpath `dirname "$0"`)/dacapo.sh $work_dir "-Dchappie.rate=8" $bench "--size $size --iterations $iters"
  done
done

benchs=(
  avrora
  batik
  eclipse
  h2
  pmd
  sunflow
  tomcat
)
size=default
iters=1

for bench in ${benchs[@]}; do
  mkdir ${current_data_root}/${bench}
  for i in `seq 0 3`; do
    work_dir=${current_data_root}/${bench}/${i}
    sudo rm -rf $work_dir
    $(realpath `dirname "$0"`)/dacapo.sh $work_dir "-Dchappie.rate=128" $bench "--size $size --iterations $iters"
  done
done

benchs=(
  graphchi
)
size=default
iters=1

for bench in ${benchs[@]}; do
  mkdir ${current_data_root}/${bench}
  for i in `seq 0 3`; do
    work_dir=${current_data_root}/${bench}/${i}
    sudo rm -rf $work_dir
    $(realpath `dirname "$0"`)/dacapo.sh $work_dir "-Dchappie.rate=16" $bench "--size $size --iterations $iters"
  done
done
