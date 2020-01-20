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

parsec=/home/timur/projects/parsec/parsec-3.0/bin/parsecmgmt

# real args
current_data_root=$data_root/2020/precision
mkdir $current_data_root

vm_rates=(1 2 4 8)
os_rates=(4 8 16 32)

benchs=(
  biojava
  jython
  xalan
)
size=default
iters=10

for bench in ${benchs[@]}; do
  mkdir ${current_data_root}/${bench}
  for vm in ${vm_rates[@]}; do
    for os in ${os_rates[@]}; do
      if [[ ($vm == 1) && ($os == 4) ]]; then
        echo skipping
      else
        mkdir ${current_data_root}/${bench}/${vm}-${os}
        for i in `seq 0 3`; do
          echo $vm-$os
          work_dir=${current_data_root}/${bench}/${vm}-${os}/${i}
          echo $work_dir
          sudo rm -rf $work_dir
          ./dacapo.sh $work_dir "-Dchappie.vm=$vm -Dchappie.os=$os -Dchappie.trace=$trace" $bench "--size $size --iterations $iters"
        done
      fi
    done
  done
done

benchs=(
  fop
  jme
  kafka
)
size=default
iters=100

for bench in ${benchs[@]}; do
  mkdir ${current_data_root}/${bench}
  for vm in ${vm_rates[@]}; do
    for os in ${os_rates[@]}; do
      if [[ ($vm == 1) && ($os == 4) ]]; then
        echo skipping
      else
        mkdir ${current_data_root}/${bench}/${vm}-${os}
        for i in `seq 0 3`; do
          echo $vm-$os
          work_dir=${current_data_root}/${bench}/${vm}-${os}/${i}
          echo $work_dir
          sudo rm -rf $work_dir
          ./dacapo.sh $work_dir "-Dchappie.vm=$vm -Dchappie.os=$os -Dchappie.trace=$trace" $bench "--size $size --iterations $iters"
        done
      fi
    done
  done
done

benchs=(
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
  mkdir ${current_data_root}/${bench}
  for vm in ${vm_rates[@]}; do
    for os in ${os_rates[@]}; do
      if [[ ($vm == 1) && ($os == 4) ]]; then
        echo skipping
      else
        mkdir ${current_data_root}/${bench}/${vm}-${os}
        for i in `seq 0 3`; do
          echo $vm-$os
          work_dir=${current_data_root}/${bench}/${vm}-${os}/${i}
          echo $work_dir
          sudo rm -rf $work_dir
          ./dacapo.sh $work_dir "-Dchappie.vm=$vm -Dchappie.os=$os -Dchappie.trace=$trace" $bench "--size $size --iterations $iters"
        done
      fi
    done
  done
done

# benchs=(
#   graphchi
# )
# size=huge
# iters=10
#
# for bench in ${benchs[@]}; do
#   mkdir ${current_data_root}/${bench}
#   for vm in ${vm_rates[@]}; do
#     for os in ${os_rates[@]}; do
#       if [[ ($vm == 1) && ($os == 4) ]]; then
#         echo skipping
#       else
#         mkdir ${current_data_root}/${bench}/${vm}-${os}
#         for i in `seq 0 3`; do
#           echo $vm-$os
#           work_dir=${current_data_root}/${bench}/${vm}-${os}/${i}
#           echo $work_dir
#           sudo rm -rf $work_dir
#           ./dacapo.sh $work_dir "-Dchappie.vm=$vm -Dchappie.os=$os -Dchappie.trace=$trace" $bench "--size $size --iterations $iters"
#         done
#       fi
#     done
#   done
# done

for bench_dir in $current_data_root/*; do
  for case_dir in $bench_dir/*; do
    for work_dir in $case_dir/*; do
      echo $work_dir
      python3 src/python/analysis -d $work_dir &
    done
  done
  wait $!
done
