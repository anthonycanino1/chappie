#!/bin/bash

chappie_root=$(realpath `dirname "$0"`)

jars=wrapper/chappie-util.jar:wrapper/jar/dacapo-evaluation-git.jar
main=Harness

parsec=/home/timur/projects/parsec/parsec-3.0/bin/parsecmgmt

benchs=(biojava jython kafka xalan)
size=default
iters=10

for bench in ${benchs[@]}; do
  echo $bench
  work_dir=${chappie_root}/../chappie-data/foreign/${bench}
  java_args="${bench} --callback chappie_util.wrapper.DaCapo --size ${size} --iterations ${iters} --scratch-directory ${work_dir}/dacapo-scratch"

  ${chappie_root}/chappie.sh -d ${work_dir} -Dchappie.trace=0 -Dchappie.os=4 -cp $jars $main $java_args &
  pid=$!

  while kill -0 $pid 2> /dev/null; do
    $parsec -a run -p ferret -i simlarge > /dev/null 2> /dev/null
  done
done

benchs=(fop jme)
size=default
iters=50

for bench in ${benchs[@]}; do
  echo $bench
  work_dir=${chappie_root}/../chappie-data/foreign/${bench}
  java_args="${bench} --callback chappie_util.wrapper.DaCapo --size ${size} --iterations ${iters} --scratch-directory ${work_dir}/dacapo-scratch"

  ${chappie_root}/chappie.sh -d ${work_dir} -Dchappie.trace=0 -Dchappie.os=4 -cp $jars $main $java_args &
  pid=$!

  while kill -0 $pid 2> /dev/null; do
    $parsec -a run -p ferret -i simlarge > /dev/null 2> /dev/null
  done
done

benchs=(batik eclipse pmd sunflow)
size=large
iters=10

for bench in ${benchs[@]}; do
  echo $bench
  work_dir=${chappie_root}/../chappie-data/foreign/${bench}
  java_args="${bench} --callback chappie_util.wrapper.DaCapo --size ${size} --iterations ${iters} --scratch-directory ${work_dir}/dacapo-scratch"

  ${chappie_root}/chappie.sh -d ${work_dir} -Dchappie.trace=0 -Dchappie.os=4 -cp $jars $main $java_args &
  pid=$!

  while kill -0 $pid 2> /dev/null; do
    $parsec -a run -p ferret -i simlarge > /dev/null 2> /dev/null
  done
done

benchs=(lusearch)
size=huge
iters=10

for bench in ${benchs[@]}; do
  echo $bench
  work_dir=${chappie_root}/../chappie-data/foreign/${bench}
  java_args="${bench} --callback chappie_util.wrapper.DaCapo --size ${size} --iterations ${iters} --scratch-directory ${work_dir}/dacapo-scratch"

  ${chappie_root}/chappie.sh -d ${work_dir} -Dchappie.trace=0 -Dchappie.os=4 -cp $jars $main $java_args &
  pid=$!

  while kill -0 $pid 2> /dev/null; do
    $parsec -a run -p ferret -i simlarge > /dev/null 2> /dev/null
  done
done

benchs=(graphchi)
size=huge
iters=5

for bench in ${benchs[@]}; do
  echo $bench
  work_dir=${chappie_root}/../chappie-data/foreign/${bench}
  java_args="${bench} --callback chappie_util.wrapper.DaCapo --size ${size} --iterations ${iters} --scratch-directory ${work_dir}/dacapo-scratch"

  ${chappie_root}/chappie.sh -d ${work_dir} -Dchappie.trace=0 -Dchappie.os=4 -cp $jars $main $java_args &
  pid=$!

  while kill -0 $pid 2> /dev/null; do
    $parsec -a run -p ferret -i simlarge > /dev/null 2> /dev/null
  done
done
