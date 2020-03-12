#!/bin/bash
chappie_root=$(realpath `dirname "$0"`)
data_root=/home/timur/projects/chappie-data

# guard arguments in case i screw up
work_dir=./chappie-logs

rate=1
os=4
trace=1000000

bench=graphchi
size=default
iters=10

# real args
current_data_root=$data_root/fse2020/calmness
mkdir $current_data_root

rates=(0 1 2 4 8 16 32 64 128 256 512)

benchs=(
  biojava
  jython
  xalan
)
size=default
iters=10

for bench in ${benchs[@]}; do
  mkdir ${current_data_root}/${bench}
  for rate in ${rates[@]}; do
    if [ $rate == "0" ]; then
      work_dir=$data_root/fse2020/freq/${bench}
      echo $work_dir

      sudo rm -rf $work_dir
      mkdir $work_dir
      ./dacapo.sh $work_dir "-Dchappie.rate=$rate" $bench "--size $size --iterations $iters"
    else
      work_dir=${current_data_root}/${bench}/${rate}
      echo $work_dir

      sudo rm -rf $work_dir
      mkdir $work_dir
      os_rate=$((512 / $rate))
      ./dacapo.sh $work_dir "-Dchappie.rate=$rate -Dchappie.os=$os_rate" $bench "--size $size --iterations $iters"
    fi
  done
done

benchs=(
  fop
  jme
  # kafka
)
size=default
iters=100

for bench in ${benchs[@]}; do
  mkdir ${current_data_root}/${bench}10
  for rate in ${rates[@]}; do
    if [ $rate == "0" ]; then
      work_dir=$data_root/fse2020/freq/${bench}10
      echo $work_dir

      sudo rm -rf $work_dir
      mkdir $work_dir
      ./dacapo.sh $work_dir "-Dchappie.rate=$rate" $bench "--size $size --iterations $iters"
    else
      work_dir=${current_data_root}/${bench}10/${rate}
      echo $work_dir

      sudo rm -rf $work_dir
      mkdir $work_dir
      os_rate=$((512 / $rate))
      ./dacapo.sh $work_dir "-Dchappie.rate=$rate -Dchappie.os=$os_rate" $bench "--size $size --iterations $iters"
    fi
  done
done

benchs=(
  avrora
  batik
  eclipse
  # h2
  pmd
  # sunflow
  tomcat
)
size=large
iters=10

for bench in ${benchs[@]}; do
  mkdir ${current_data_root}/${bench}
  for rate in ${rates[@]}; do
    if [ $rate == "0" ]; then
      work_dir=$data_root/fse2020/freq/${bench}
      echo $work_dir

      sudo rm -rf $work_dir
      mkdir $work_dir
      ./dacapo.sh $work_dir "-Dchappie.rate=$rate" $bench "--size $size --iterations $iters"
    else
      work_dir=${current_data_root}/${bench}/${rate}
      echo $work_dir

      sudo rm -rf $work_dir
      mkdir $work_dir
      os_rate=$((512 / $rate))
      ./dacapo.sh $work_dir "-Dchappie.rate=$rate -Dchappie.os=$os_rate" $bench "--size $size --iterations $iters"
    fi
  done
done

benchs=(
  graphchi
)
size=huge
iters=10

for bench in ${benchs[@]}; do
  mkdir ${current_data_root}/${bench}
  for rate in ${rates[@]}; do
    if [ $rate == "0" ]; then
      work_dir=$data_root/fse2020/freq/${bench}
      echo $work_dir

      sudo rm -rf $work_dir
      mkdir $work_dir
      ./dacapo.sh $work_dir "-Dchappie.rate=$rate" $bench "--size $size --iterations $iters"
    else
      work_dir=${current_data_root}/${bench}/${rate}
      echo $work_dir

      sudo rm -rf $work_dir
      mkdir $work_dir
      os_rate=$((512 / $rate))
      ./dacapo.sh $work_dir "-Dchappie.rate=$rate -Dchappie.os=$os_rate" $bench "--size $size --iterations $iters"
    fi
  done
done
