#!/bin/bash
chappie_root=$(realpath `dirname "$0"`)
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
current_data_root=$data_root/2020/submillis-2
mkdir $current_data_root

rate=500000
trace=500000
os=8

# # benchs=(
# #   biojava
# #   jython
# #   xalan
# # )
# # size=default
# # iters=10
# #
# # for bench in ${benchs[@]}; do
# #   mkdir ${current_data_root}/${bench}
# #   for i in `seq 0 3`; do
# #     work_dir=${current_data_root}/${bench}/${i}
# #     sudo rm -rf $work_dir
# #     ./dacapo.sh $work_dir "-Dchappie.rate=$rate -Dchappie.vm=$vm -Dchappie.os=$os -Dchappie.trace=$trace" $bench "--size $size --iterations $iters"
# #   done
# # done
#
# benchs=(
#   # fop
#   jme
#   # kafka
# )
# size=default
# iters=100
#
# for bench in ${benchs[@]}; do
#   mkdir ${current_data_root}/${bench}
#   for i in `seq 0 3`; do
#     work_dir=${current_data_root}/${bench}/${i}
#     sudo rm -rf $work_dir
#     ./dacapo.sh $work_dir "-Dchappie.rate=$rate -Dchappie.vm=$vm -Dchappie.os=$os -Dchappie.trace=$trace" $bench "--size $size --iterations $iters"
#   done
# done
#
benchs=(
  # batik
  # eclipse
  # h2
  # pmd
  # sunflow
  tomcat
)
size=large
iters=10

for bench in ${benchs[@]}; do
  mkdir ${current_data_root}/${bench}
  for i in `seq 0 3`; do
    work_dir=${current_data_root}/${bench}/${i}
    sudo rm -rf $work_dir
    ./dacapo.sh $work_dir "-Dchappie.rate=$rate -Dchappie.vm=$vm -Dchappie.os=$os -Dchappie.trace=$trace" $bench "--size $size --iterations $iters"
  done
done

# benchs=(
#   graphchi
# )
# size=huge
# iters=10
# #
# for bench in ${benchs[@]}; do
#   mkdir ${current_data_root}/${bench}
#   for i in `seq 0 3`; do
#     work_dir=${current_data_root}/${bench}/${i}
#     sudo rm -rf $work_dir
#     ./dacapo.sh $work_dir "-Dchappie.rate=$rate -Dchappie.vm=$vm -Dchappie.os=$os -Dchappie.trace=$trace" $bench "--size $size --iterations $iters"
#   done
# done

benchs=(tomcat)

for bench in ${benchs[@]}; do
  case_dir=${current_data_root}/${bench}
  for work_dir in $case_dir/*; do
    echo $work_dir
    python3 src/python/analysis -d $work_dir &
  done
done

# for case_dir in $current_data_root/*; do
#   for work_dir in $case_dir/*; do
#     echo $work_dir
#     python3 src/python/analysis -d $work_dir &
#   done
#   wait $!
# done
