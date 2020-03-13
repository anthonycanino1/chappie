#!/bin/bash
chappie_root=$(realpath `dirname "$0"`)/../..
# this should be changed to the location desired
data_root=/home/timur/projects/chappie-data
mkdir $data_root

data_root=$data_root/fse2020
mkdir $data_root

freq_data_root=$data_root/freq
mkdir $freq_data_root

calm_data_root=$data_root/calmness
mkdir $calm_data_root

rates=(0 1 2 4 8 16 32 64 128 256 512)

benchs=(
  biojava
  jython
  xalan
)
size=default
iters=10

for bench in ${benchs[@]}; do
  mkdir ${freq_data_root}/${bench}
  mkdir ${calm_data_root}/${bench}
  for rate in ${rates[@]}; do
    if [ $rate == "0" ]; then
      work_dir=${freq_data_root}/${bench}
      echo $work_dir

      rm -rf $work_dir
      mkdir $work_dir
      $(realpath `dirname "$0"`)/dacapo.sh $work_dir "-Dchappie.rate=0" $bench "--size $size --iterations $iters"
    else
      work_dir=${calm_data_root}/${bench}/${rate}
      echo $work_dir

      rm -rf $work_dir
      mkdir $work_dir
      freq_rate=$((512 / $rate))
      $(realpath `dirname "$0"`)/dacapo.sh $work_dir "-Dchappie.rate=$rate -Dchappie.os=0 -Dchappie.freq=$freq_rate" $bench "--size $size --iterations $iters"
    fi
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
size=large
iters=10

for bench in ${benchs[@]}; do
  mkdir ${freq_data_root}/${bench}
  mkdir ${calm_data_root}/${bench}
  for rate in ${rates[@]}; do
    if [ $rate == "0" ]; then
      work_dir=${freq_data_root}/${bench}
      echo $work_dir

      rm -rf $work_dir
      mkdir $work_dir
      $(realpath `dirname "$0"`)/dacapo.sh $work_dir "-Dchappie.rate=0" $bench "--size $size --iterations $iters"
    else
      work_dir=${calm_data_root}/${bench}/${rate}
      echo $work_dir

      rm -rf $work_dir
      mkdir $work_dir
      freq_rate=$((512 / $rate))
      $(realpath `dirname "$0"`)/dacapo.sh $work_dir "-Dchappie.rate=$rate -Dchappie.os=0 -Dchappie.freq=$freq_rate" $bench "--size $size --iterations $iters"
    fi
  done
done

benchs=(
  graphchi
)
size=huge
iters=10

for bench in ${benchs[@]}; do
  mkdir ${freq_data_root}/${bench}
  mkdir ${calm_data_root}/${bench}
  for rate in ${rates[@]}; do
    if [ $rate == "0" ]; then
      work_dir=${freq_data_root}/${bench}
      echo $work_dir

      rm -rf $work_dir
      mkdir $work_dir
      $(realpath `dirname "$0"`)/dacapo.sh $work_dir "-Dchappie.rate=0" $bench "--size $size --iterations $iters"
    else
      work_dir=${calm_data_root}/${bench}/${rate}
      echo $work_dir

      rm -rf $work_dir
      mkdir $work_dir
      freq_rate=$((512 / $rate))
      $(realpath `dirname "$0"`)/dacapo.sh $work_dir "-Dchappie.rate=$rate -Dchappie.os=0 -Dchappie.freq=$freq_rate" $bench "--size $size --iterations $iters"
    fi
  done
done
