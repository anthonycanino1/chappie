#!/bin/bash
chappie_root=$(realpath `dirname "$0"`)

jars=wrapper/chappie-util.jar:wrapper/jar/dacapo-evaluation-git.jar
main=Harness

bench=$1
iters=$2
size=$3

work_dir=/home/timur/projects/pldi-2020/baseline/${bench}/nop
echo $work_dir
java_args="${bench} --callback chappie_util.wrapper.DaCapo --size ${size} --iterations ${iters} --scratch-directory ${work_dir}/dacapo-scratch"
${chappie_root}/chappie.sh -d ${work_dir} -Dchappie.rate=0 -cp $jars $main $java_args

vm_parms=(1 2 4 8)
os_parms=(4 8 16 32)
for vm in ${vm_parms[@]}; do
  for os in ${os_parms[@]}; do
    work_dir=/home/timur/projects/pldi-2020/baseline/${bench}/${vm}-${os}
    echo $work_dir
    java_args="${bench} --callback chappie_util.wrapper.DaCapo --size ${size} --iterations ${iters} --scratch-directory ${work_dir}/dacapo-scratch"

    ${chappie_root}/chappie.sh -d ${work_dir} -Dchappie.vm=$vm -Dchappie.os=$os -cp $jars $main $java_args
  done
done

parsec=/home/timur/projects/parsec/parsec-3.0/bin/parsecmgmt

work_dir=/home/timur/projects/pldi-2020/baseline/${bench}/foreign
echo $work_dir
java_args="${bench} --callback chappie_util.wrapper.DaCapo --size ${size} --iterations ${iters} --scratch-directory ${work_dir}/dacapo-scratch"

${chappie_root}/chappie.sh -d ${work_dir} -Dchappie.os=4 -cp $jars $main $java_args &
pid=$!

while kill -0 $pid 2> /dev/null; do
  $parsec -a run -p ferret -i simlarge > /dev/null 2> /dev/null
done
