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
current_data_root=$data_root/2020/homo-coapp-nop
mkdir $current_data_root

benchs=(
  # biojava
  jython
)
size=default
iters=10

for bench in ${benchs[@]}; do
  mkdir ${current_data_root}/${bench}
  mkdir ${current_data_root}/${bench}/${bench}_0
  mkdir ${current_data_root}/${bench}/${bench}_1
  work_dir=${current_data_root}/${bench}/${bench}_0
  sudo rm -rf $work_dir
  ./dacapo.sh $work_dir "-Dchappie.rate=0" $bench "--size $size --iterations $iters" &
  id1=$!

  work_dir=${current_data_root}/${bench}/${bench}_1
  sudo rm -rf $work_dir
  ./dacapo.sh $work_dir "-Dchappie.rate=0" $bench "--size $size --iterations $iters" &
  id2=$!

  wait $id1
  wait $id2
done

# benchs=(
#   batik
#   h2
#   sunflow
# )
# size=large
# iters=10
#
# for bench in ${benchs[@]}; do
#   mkdir ${current_data_root}/${bench}
#   mkdir ${current_data_root}/${bench}/${bench}_0
#   mkdir ${current_data_root}/${bench}/${bench}_1
#   work_dir=${current_data_root}/${bench}/${bench}_0
#   sudo rm -rf $work_dir
#   ./dacapo.sh $work_dir "-Dchappie.rate=0" $bench "--size $size --iterations $iters" &
#   id1=$!
#
#   work_dir=${current_data_root}/${bench}/${bench}_1
#   sudo rm -rf $work_dir
#   ./dacapo.sh $work_dir "-Dchappie.rate=0" $bench "--size $size --iterations $iters" &
#   id2=$!
#
#   wait $id1
#   wait $id2
# done
#
# benchs=(
#   graphchi
# )
# size=huge
# iters=10
#
# for bench in ${benchs[@]}; do
#   mkdir ${current_data_root}/${bench}
#   mkdir ${current_data_root}/${bench}/${bench}_0
#   mkdir ${current_data_root}/${bench}/${bench}_1
#   work_dir=${current_data_root}/${bench}/${bench}_0
#   sudo rm -rf $work_dir
#   ./dacapo.sh $work_dir "-Dchappie.rate=0" $bench "--size $size --iterations $iters" &
#   id1=$!
#
#   work_dir=${current_data_root}/${bench}/${bench}_1
#   sudo rm -rf $work_dir
#   ./dacapo.sh $work_dir "-Dchappie.rate=0" $bench "--size $size --iterations $iters" &
#   id2=$!
#
#   wait $id1
#   wait $id2
# done

# current_data_root=$data_root/2020/hetero-coapp-nop
# mkdir $current_data_root
#
# bench1=batik
# size1=large
# iters1=10
#
# bench2=biojava
# size2=default
# iters2=10
#
# mkdir ${current_data_root}/${bench1}_${bench2}
# mkdir ${current_data_root}/${bench1}_${bench2}/${bench1}_0
# mkdir ${current_data_root}/${bench1}_${bench2}/${bench2}_1
#
# work_dir=${current_data_root}/${bench1}_${bench2}/${bench1}_0
# sudo rm -rf $work_dir
# ./dacapo.sh $work_dir "-Dchappie.rate=0" $bench1 "--size $size1 --iterations $iters1" &
# id1=$!
#
# work_dir=${current_data_root}/${bench1}_${bench2}/${bench2}_1
# sudo rm -rf $work_dir
# ./dacapo.sh $work_dir "-Dchappie.rate=0" $bench2 "--size $size2 --iterations $iters2" &
# id2=$!
#
# wait $id1
# wait $id2
#
# bench1=biojava
# size1=default
# iters1=10
#
# bench2=eclipse
# size2=large
# iters2=10
#
# mkdir ${current_data_root}/${bench1}_${bench2}
# mkdir ${current_data_root}/${bench1}_${bench2}/${bench1}_0
# mkdir ${current_data_root}/${bench1}_${bench2}/${bench2}_1
#
# work_dir=${current_data_root}/${bench1}_${bench2}/${bench1}_0
# sudo rm -rf $work_dir
# ./dacapo.sh $work_dir "-Dchappie.rate=0" $bench1 "--size $size1 --iterations $iters1" &
# id1=$!
#
# work_dir=${current_data_root}/${bench1}_${bench2}/${bench2}_1
# sudo rm -rf $work_dir
# ./dacapo.sh $work_dir "-Dchappie.rate=0" $bench2 "--size $size2 --iterations $iters2" &
# id2=$!
#
# wait $id1
# wait $id2
#
# bench1=biojava
# size1=default
# iters1=10
#
# bench2=jython
# size2=default
# iters2=10
#
# mkdir ${current_data_root}/${bench1}_${bench2}
# mkdir ${current_data_root}/${bench1}_${bench2}/${bench1}_0
# mkdir ${current_data_root}/${bench1}_${bench2}/${bench2}_1
#
# work_dir=${current_data_root}/${bench1}_${bench2}/${bench1}_0
# sudo rm -rf $work_dir
# ./dacapo.sh $work_dir "-Dchappie.rate=0" $bench1 "--size $size1 --iterations $iters1" &
# id1=$!
#
# work_dir=${current_data_root}/${bench1}_${bench2}/${bench2}_1
# sudo rm -rf $work_dir
# ./dacapo.sh $work_dir "-Dchappie.rate=0" $bench2 "--size $size2 --iterations $iters2" &
# id2=$!
#
# wait $id1
# wait $id2
#
# bench1=h2
# size1=large
# iters1=10
#
# bench2=sunflow
# size2=large
# iters2=10
#
# mkdir ${current_data_root}/${bench1}_${bench2}
# mkdir ${current_data_root}/${bench1}_${bench2}/${bench1}_0
# mkdir ${current_data_root}/${bench1}_${bench2}/${bench2}_1
#
# work_dir=${current_data_root}/${bench1}_${bench2}/${bench1}_0
# sudo rm -rf $work_dir
# ./dacapo.sh $work_dir "-Dchappie.rate=0" $bench1 "--size $size1 --iterations $iters1" &
# id1=$!
#
# work_dir=${current_data_root}/${bench1}_${bench2}/${bench2}_1
# sudo rm -rf $work_dir
# ./dacapo.sh $work_dir "-Dchappie.rate=0" $bench2 "--size $size2 --iterations $iters2" &
# id2=$!
#
# wait $id1
# wait $id2
#
# bench1=pmd
# size1=large
# iters1=10
#
# bench2=sunflow
# size2=large
# iters2=10
#
# mkdir ${current_data_root}/${bench1}_${bench2}
# mkdir ${current_data_root}/${bench1}_${bench2}/${bench1}_0
# mkdir ${current_data_root}/${bench1}_${bench2}/${bench2}_1
#
# work_dir=${current_data_root}/${bench1}_${bench2}/${bench1}_0
# sudo rm -rf $work_dir
# ./dacapo.sh $work_dir "-Dchappie.rate=0" $bench1 "--size $size1 --iterations $iters1" &
# id1=$!
#
# work_dir=${current_data_root}/${bench1}_${bench2}/${bench2}_1
# sudo rm -rf $work_dir
# ./dacapo.sh $work_dir "-Dchappie.rate=0" $bench2 "--size $size2 --iterations $iters2" &
# id2=$!
#
# wait $id1
# wait $id2

# bench1=batik
# size1=large
# iters1=10
#
# bench2=biojava
# size2=default
# iters2=10
#
# mkdir ${current_data_root}/${bench1}_${bench2}
# mkdir ${current_data_root}/${bench1}_${bench2}/${bench1}_0
# mkdir ${current_data_root}/${bench1}_${bench2}/${bench2}_1
#
# for i in `seq 0 3`; do
#   work_dir=${current_data_root}/${bench1}_${bench2}/${bench1}_0/${i}
#   sudo rm -rf $work_dir
#   ./dacapo.sh $work_dir "-Dchappie.vm=$vm -Dchappie.os=$os -Dchappie.trace=$trace" $bench1 "--size $size1 --iterations $iters1" &
#   id1=$!
#
#   work_dir=${current_data_root}/${bench1}_${bench2}/${bench2}_1/${i}
#   sudo rm -rf $work_dir
#   ./dacapo.sh $work_dir "-Dchappie.vm=$vm -Dchappie.os=$os -Dchappie.trace=$trace" $bench2 "--size $size2 --iterations $iters2" &
#   id2=$!
#
#   wait $id1
#   wait $id2
# done
#
# bench1=biojava
# size1=default
# iters1=10
#
# bench2=eclipse
# size2=large
# iters2=10
#
# mkdir ${current_data_root}/${bench1}_${bench2}
# mkdir ${current_data_root}/${bench1}_${bench2}/${bench1}_0
# mkdir ${current_data_root}/${bench1}_${bench2}/${bench2}_1
#
# for i in `seq 0 3`; do
#   work_dir=${current_data_root}/${bench1}_${bench2}/${bench1}_0/${i}
#   sudo rm -rf $work_dir
#   ./dacapo.sh $work_dir "-Dchappie.vm=$vm -Dchappie.os=$os -Dchappie.trace=$trace" $bench1 "--size $size1 --iterations $iters1" &
#   id1=$!
#
#   work_dir=${current_data_root}/${bench1}_${bench2}/${bench2}_1/${i}
#   sudo rm -rf $work_dir
#   ./dacapo.sh $work_dir "-Dchappie.vm=$vm -Dchappie.os=$os -Dchappie.trace=$trace" $bench2 "--size $size2 --iterations $iters2" &
#   id2=$!
#
#   wait $id1
#   wait $id2
# done
#
# bench1=biojava
# size1=default
# iters1=10
#
# bench2=jython
# size2=default
# iters2=10
#
# mkdir ${current_data_root}/${bench1}_${bench2}
# mkdir ${current_data_root}/${bench1}_${bench2}/${bench1}_0
# mkdir ${current_data_root}/${bench1}_${bench2}/${bench2}_1
#
# for i in `seq 0 3`; do
#   work_dir=${current_data_root}/${bench1}_${bench2}/${bench1}_0/${i}
#   sudo rm -rf $work_dir
#   ./dacapo.sh $work_dir "-Dchappie.vm=$vm -Dchappie.os=$os -Dchappie.trace=$trace" $bench1 "--size $size1 --iterations $iters1" &
#   id1=$!
#
#   work_dir=${current_data_root}/${bench1}_${bench2}/${bench2}_1/${i}
#   sudo rm -rf $work_dir
#   ./dacapo.sh $work_dir "-Dchappie.vm=$vm -Dchappie.os=$os -Dchappie.trace=$trace" $bench2 "--size $size2 --iterations $iters2" &
#   id2=$!
#
#   wait $id1
#   wait $id2
# done
#
# bench1=h2
# size1=large
# iters1=10
#
# bench2=sunflow
# size2=large
# iters2=10
#
# mkdir ${current_data_root}/${bench1}_${bench2}
# mkdir ${current_data_root}/${bench1}_${bench2}/${bench1}_0
# mkdir ${current_data_root}/${bench1}_${bench2}/${bench2}_1
#
# for i in `seq 0 3`; do
#   work_dir=${current_data_root}/${bench1}_${bench2}/${bench1}_0/${i}
#   sudo rm -rf $work_dir
#   ./dacapo.sh $work_dir "-Dchappie.vm=$vm -Dchappie.os=$os -Dchappie.trace=$trace" $bench1 "--size $size1 --iterations $iters1" &
#   id1=$!
#
#   work_dir=${current_data_root}/${bench1}_${bench2}/${bench2}_1/${i}
#   sudo rm -rf $work_dir
#   ./dacapo.sh $work_dir "-Dchappie.vm=$vm -Dchappie.os=$os -Dchappie.trace=$trace" $bench2 "--size $size2 --iterations $iters2" &
#   id2=$!
#
#   wait $id1
#   wait $id2
# done
#
# bench1=pmd
# size1=large
# iters1=10
#
# bench2=sunflow
# size2=large
# iters2=10
#
# mkdir ${current_data_root}/${bench1}_${bench2}
# mkdir ${current_data_root}/${bench1}_${bench2}/${bench1}_0
# mkdir ${current_data_root}/${bench1}_${bench2}/${bench2}_1
#
# for i in `seq 0 3`; do
#   work_dir=${current_data_root}/${bench1}_${bench2}/${bench1}_0/${i}
#   sudo rm -rf $work_dir
#   ./dacapo.sh $work_dir "-Dchappie.vm=$vm -Dchappie.os=$os -Dchappie.trace=$trace" $bench1 "--size $size1 --iterations $iters1" &
#   id1=$!
#
#   work_dir=${current_data_root}/${bench1}_${bench2}/${bench2}_1/${i}
#   sudo rm -rf $work_dir
#   ./dacapo.sh $work_dir "-Dchappie.vm=$vm -Dchappie.os=$os -Dchappie.trace=$trace" $bench2 "--size $size2 --iterations $iters2" &
#   id2=$!
#
#   wait $id1
#   wait $id2
# done

current_data_root=$data_root/2020/homo-coapp-nop

for bench_dir in $current_data_root/*; do
    for work_dir in $bench_dir/*; do
      echo $work_dir
      python3 src/python/analysis -d $work_dir &
  done
done

# current_data_root=$data_root/2020/hetero-coapp-nop
#
# for bench_dir in $current_data_root/*; do
#     for work_dir in $bench_dir/*; do
#       echo $work_dir
#       python3 src/python/analysis -d $work_dir &
#   done
# done
