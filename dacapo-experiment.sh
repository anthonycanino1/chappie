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
current_data_root=$data_root/fse2020/baseline
mkdir $current_data_root

# benchs=(
#   biojava
#   jython
# #   xalan
# )
# size=default
# iters=10
#
# for bench in ${benchs[@]}; do
#   mkdir ${current_data_root}/${bench}
#   for i in `seq 2 2`; do
#     work_dir=${current_data_root}/${bench}/${i}
#     sudo rm -rf $work_dir
#     ./dacapo.sh $work_dir "-Dchappie.rate=8" $bench "--size $size --iterations $iters"
#   done
# done

# benchs=(
#   # biojava
#   # jython
#   xalan
# )
# size=default
# iters=10
#
# for bench in ${benchs[@]}; do
#   mkdir ${current_data_root}/${bench}
#   for i in `seq 1 1`; do
#     work_dir=${current_data_root}/${bench}/${i}
#     sudo rm -rf $work_dir
#     ./dacapo.sh $work_dir "-Dchappie.rate=16" $bench "--size $size --iterations $iters"
#   done
# done

# benchs=(
#   fop
#   # jme
#   # kafka
# )
# size=default
# iters=100
#
# for bench in ${benchs[@]}; do
#   mkdir ${current_data_root}/${bench}
#   for i in `seq 2 2`; do
#     work_dir=${current_data_root}/${bench}/${i}
#     sudo rm -rf $work_dir
#     ./dacapo.sh $work_dir "-Dchappie.rate=4" $bench "--size $size --iterations $iters"
#   done
# done

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
#   for i in `seq 1 1`; do
#     work_dir=${current_data_root}/${bench}/${i}
#     sudo rm -rf $work_dir
#     ./dacapo.sh $work_dir "-Dchappie.rate=128" $bench "--size $size --iterations $iters"
#   done
# done
#
# benchs=(
#   # fop
#   # jme
#   kafka
# )
# size=default
# iters=100
#
# for bench in ${benchs[@]}; do
#   mkdir ${current_data_root}/${bench}
#   for i in `seq 1 1`; do
#     work_dir=${current_data_root}/${bench}/${i}
#     sudo rm -rf $work_dir
#     ./dacapo.sh $work_dir "-Dchappie.rate=64" $bench "--size $size --iterations $iters"
#   done
# done

# benchs=(
#   avrora
#   # batik
#   # eclipse
#   # h2
#   # pmd
#   # sunflow
#   # tomcat
# )
# size=large
# iters=10
#
# for bench in ${benchs[@]}; do
#   mkdir ${current_data_root}/${bench}
#   for i in `seq 6 9`; do
#     work_dir=${current_data_root}/${bench}/${i}
#     sudo rm -rf $work_dir
#     ./dacapo.sh $work_dir "-Dchappie.rate=128" $bench "--size $size --iterations $iters"
#   done
# done

# benchs=(
#   # avrora
#   batik
#   # eclipse
#   # h2
#   # pmd
#   # sunflow
#   # tomcat
# )
# size=large
# iters=10
#
# for bench in ${benchs[@]}; do
#   mkdir ${current_data_root}/${bench}
#   for i in `seq 1 1`; do
#     work_dir=${current_data_root}/${bench}/${i}
#     sudo rm -rf $work_dir
#     ./dacapo.sh $work_dir "-Dchappie.rate=8" $bench "--size $size --iterations $iters"
#   done
# done

# benchs=(
#   # avrora
#   # batik
#   eclipse
#   # h2
#   pmd
#   sunflow
#   tomcat
# )
# size=large
# iters=10
#
# for bench in ${benchs[@]}; do
#   mkdir ${current_data_root}/${bench}
#   for i in `seq 3 3`; do
#     work_dir=${current_data_root}/${bench}/${i}
#     sudo rm -rf $work_dir
#     ./dacapo.sh $work_dir "-Dchappie.rate=16" $bench "--size $size --iterations $iters"
#   done
# done

benchs=(
  # avrora
  # batik
  # eclipse
  h2
  # pmd
  # sunflow
  # tomcat
)
size=large
iters=10

for bench in ${benchs[@]}; do
  mkdir ${current_data_root}/${bench}
  for i in `seq 3 3`; do
    work_dir=${current_data_root}/${bench}/${i}
    sudo rm -rf $work_dir
    ./dacapo.sh $work_dir "-Dchappie.rate=32" $bench "--size $size --iterations $iters"
  done
done

benchs=(
  graphchi
)
size=huge
iters=10

for bench in ${benchs[@]}; do
  mkdir ${current_data_root}/${bench}
  for i in `seq 2 4`; do
    work_dir=${current_data_root}/${bench}/${i}
    sudo rm -rf $work_dir
    ./dacapo.sh $work_dir "-Dchappie.rate=16" $bench "--size $size --iterations $iters"
  done
done
