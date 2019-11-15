#!/bin/bash

chappie_root=$(realpath `dirname "$0"`)

jars=wrapper/chappie-util.jar:wrapper/jar/dacapo-evaluation-git.jar

# iters=10
# bench=h2
# size=large

bench=$1
iters=$2
size=$3

vm=1
os=4

work_dir=${chappie_root}/../chappie-data/stop/${bench}
# work_dir=${chappie_root}/../chappie-data/baseline/${bench}/1-4
java_args="${bench} --callback chappie_util.wrapper.DaCapo --size ${size} --iterations ${iters} --scratch-directory ${work_dir}/dacapo-scratch"

${chappie_root}/chappie.sh -d ${work_dir} -Dchappie.vm=$vm -Dchappie.os=$os -cp $jars Harness $java_args
