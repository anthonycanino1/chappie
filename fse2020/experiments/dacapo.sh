#!/bin/bash

# ./dacapo.sh work-dir "dargs" "args"
chappie_root=$(realpath `dirname "$0"`)/../..

jars=${chappie_root}/wrapper/chappie-util.jar:${chappie_root}/wrapper/jar/dacapo-evaluation-git.jar

work_dir=$1
dargs=$2
bench=$3
args=$4

dacapo_base_args="--callback chappie_util.wrapper.DaCapo --scratch-directory $work_dir/dacapo-scratch --no-validation"

chappie_call_args="-d $work_dir $dargs -cp $jars Harness $bench $args $java_args $dacapo_base_args"

# ${chappie_root}/scripts/create-chappie-call.sh $chappie_call_args
${chappie_root}/chappie.sh $chappie_call_args
