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
# current_data_root=$data_root/2020
current_data_root=$data_root/2020/baseline
benchs=(
  fop
  jme
  kafka
)

for bench in ${benchs[@]}; do
  case_dir=${current_data_root}/${bench}
  for work_dir in $case_dir/*; do
    echo $work_dir
    echo $work_dir/processed/*
    # sudo rm -rf $work_dir/processed/*
    python3 src/python/analysis -d $work_dir &
  done
done
wait $!
#
# benchs=(
#   h2
#   pmd
# )
#
# for bench in ${benchs[@]}; do
#   case_dir=${current_data_root}/${bench}
#   for work_dir in $case_dir/*; do
#     echo $work_dir
#     python3 src/python/analysis -d $work_dir &
#   done
# done
# wait $!
#
# benchs=(
#   sunflow
#   tomcat
# )
#
# for bench in ${benchs[@]}; do
#   case_dir=${current_data_root}/${bench}
#   for work_dir in $case_dir/*; do
#     echo $work_dir
#     python3 src/python/analysis -d $work_dir &
#   done
# done
# wait $!
# i=0
# current_data_root=$data_root/2020/homo-coapp
# for case_dir in $current_data_root/*; do
#   if [[ $case_dir == *"graphchi"* ]]; then
#     continue
#   fi
#
#   echo $case_dir
#
#   for bench_dir in $case_dir/*; do
#     for work_dir in $bench_dir/*; do
#       echo $work_dir
#       python3 src/python/analysis -d $work_dir &
#     done
#   done
#
#   i=$i+1
#   if [[ "$i" -eq "4" ]]; then
#     wait $!
#     i=0
#   fi
# done
#
# current_data_root=$data_root/2020/hetero-coapp
# for case_dir in $current_data_root/*; do
#   if [[ $case_dir == *"graphchi"* ]]; then
#     continue
#   fi
#
#   echo $case_dir
#
#   for bench_dir in $case_dir/*; do
#     for work_dir in $bench_dir/*; do
#       echo $work_dir
#       python3 src/python/analysis -d $work_dir &
#     done
#   done
#
#   i=$i+1
#   if [[ "$i" -eq "4" ]]; then
#     wait $!
#     i=0
#   fi
# done

# i=0
# current_data_root=$data_root/2020/homo-scale
# for case_dir in $current_data_root/*; do
#   if [[ $case_dir == *"graphchi"* ]]; then
#     continue
#   fi
#
#   for bench_dir in $case_dir/*; do
#     for work_dir in $bench_dir/*; do
#       echo $work_dir
#       python3 src/python/analysis -d $work_dir &
#     done
#   done
#   wait $!
# done
#
# current_data_root=$data_root/2020/hetero-scale
# for case_dir in $current_data_root/*; do
#   if [[ $case_dir == *"graphchi"* ]]; then
#     continue
#   fi
#
#   for bench_dir in $case_dir/*; do
#     for work_dir in $bench_dir/*; do
#       echo $work_dir
#       python3 src/python/analysis -d $work_dir &
#     done
#   done
#   wait $!
# done

# i=0
# current_data_root=$data_root/2020/foreign
# for case_dir in $current_data_root/*; do
#   if [[ $case_dir == *"graphchi"* ]]; then
#     continue
#   fi
#
#   echo $case_dir
#
#   # for bench_dir in $case_dir/*; do
#     for work_dir in $case_dir/*; do
#       echo $work_dir
#       python3 src/python/analysis -d $work_dir &
#     done
#   # done
#
#   i=$i+1
#   if [[ "$i" -eq "4" ]]; then
#     wait $!
#     i=0
#   fi
# done

# current_data_root=$data_root/2020/foreign
# for case_dir in $current_data_root/*; do
#   if [[ $bench != *"graphchi"* ]]; then
#   for bench_dir in $case_dir/*; do
#       for work_dir in $bench_dir/*; do
#         echo $work_dir
#         python3 src/python/analysis -d $work_dir &
#       done
#     done
#     wait $!
#   fi
# done
#
# current_data_root=$data_root/2020/scale
# for case_dir in $current_data_root/*; do
#   if [[ $bench != *"graphchi"* ]]; then
#   for bench_dir in $case_dir/*; do
#       for work_dir in $bench_dir/*; do
#         echo $work_dir
#         python3 src/python/analysis -d $work_dir &
#       done
#     done
#     wait $!
#   fi
# done

# work_dir=${current_data_root}/graphchi/0
# python3 src/python/analysis -d $work_dir &
# work_dir=${current_data_root}/graphchi/1
# python3 src/python/analysis -d $work_dir &
#
# wait $!
#
# work_dir=${current_data_root}/graphchi/2
# python3 src/python/analysis -d $work_dir &
# work_dir=${current_data_root}/graphchi/3
# python3 src/python/analysis -d $work_dir &
#
# wait $!

# python3 temp-scripts/bench-summary.py
# python3 temp-scripts/coapp.py
# python3 temp-scripts/foreign.py

# bench=graphchi
# size=huge
# iters=10
#
# # traces=(1000000 100000 10000)
# # traces=(1000000 500000 250000)
# traces=(500000 250000)
#
# for trace in ${traces[@]}; do
#   mkdir ${current_data_root}/${trace}
#   for i in `seq 0 4`; do
#     work_dir=${current_data_root}/${trace}/${i}
#     sudo rm -rf $work_dir
#     ./dacapo.sh $work_dir "-Dchappie.vm=$vm -Dchappie.os=$os -Dchappie.trace=$trace" $bench "--size $size --iterations $iters"
#   done
# done
#
# for case_dir in $current_data_root/*; do
#   for work_dir in $case_dir/*; do
#     python3 src/python/analysis -d $work_dir
#   done
# done
#
# wait $!

# ./dacapo.sh $work_dir "-Dchappie.vm=$vm -Dchappie.os=$os -Dchappie.trace=$trace" $bench "--size $size --iterations $iters"

# jars=wrapper/chappie-util.jar:wrapper/jar/dacapo-evaluation-git.jar
# main=Harness
#
# bench=$1
# iters=$2
# size=$3
#
# # work_dir=/home/timur/projects/pldi-2020/baseline/${bench}/nop
# # echo $work_dir
# # java_args="${bench} --callback chappie_util.wrapper.DaCapo --size ${size} --iterations ${iters} --scratch-directory ${work_dir}/dacapo-scratch"
# # ${chappie_root}/chappie.sh -d ${work_dir} -Dchappie.rate=0 -cp $jars $main $java_args
#
# vm_parms=(2 4 8)
# os_parms=(4 8 16 32)
# for vm in ${vm_parms[@]}; do
#   for os in ${os_parms[@]}; do
#     work_dir=/home/timur/projects/pldi-2020/baseline/${bench}/${vm}-${os}
#     echo $work_dir
#     java_args="${bench} --callback chappie_util.wrapper.DaCapo --size ${size} --iterations ${iters} --scratch-directory ${work_dir}/dacapo-scratch"
#
#     ${chappie_root}/chappie.sh -d ${work_dir} -Dchappie.vm=$vm -Dchappie.os=$os -cp $jars $main $java_args
#   done
# done
#
# # parsec=/home/timur/projects/parsec/parsec-3.0/bin/parsecmgmt
# #
# # work_dir=/home/timur/projects/pldi-2020/baseline/${bench}/foreign
# # echo $work_dir
# # java_args="${bench} --callback chappie_util.wrapper.DaCapo --size ${size} --iterations ${iters} --scratch-directory ${work_dir}/dacapo-scratch"
# #
# # ${chappie_root}/chappie.sh -d ${work_dir} -Dchappie.os=4 -cp $jars $main $java_args &
# # pid=$!
# #
# # while kill -0 $pid 2> /dev/null; do
# #   $parsec -a run -p ferret -i simlarge > /dev/null 2> /dev/null
# # done
