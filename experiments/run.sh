#!/bin/bash
chappie_root=$(realpath `dirname "$0"`)/..

# guard arguments in case i screw up
work_dir=./chappie-logs
# rm -rf $work_dir/*
# mkdir $work_dir

jars=${chappie_root}/chappie.jar:${chappie_root}/experiments/chappie-experiments.jar:${chappie_root}/experiments/jar/sunflow.jar:${chappie_root}/experiments/jar/janino.jar

# java -Dchappie.out=chappie-logs/base.csv -Daeneas=false -cp $jars experiments.wrapper.SunflowWrapper
# java -Dchappie.out=chappie-logs/aa.csv -Daeneas=false -Daa.min=-3 -Daa.max=3 -cp $jars experiments.wrapper.SunflowWrapper
java -Dchappie.out=chappie-logs/e10.csv -Daeneas.policy=EPSILON_GREEDY_10 -cp $jars experiments.wrapper.SunflowWrapper
java -Dchappie.out=chappie-logs/e50.csv -Daeneas.policy=EPSILON_GREEDY_50 -cp $jars experiments.wrapper.SunflowWrapper
java -Dchappie.out=chappie-logs/vbde5.csv -Daeneas.policy=VBDE_05 -cp $jars experiments.wrapper.SunflowWrapper
java -Dchappie.out=chappie-logs/vbde200.csv -Daeneas.policy=VBDE_200 -cp $jars experiments.wrapper.SunflowWrapper
