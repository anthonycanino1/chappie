#!/bin/bash

# rm -rf chappie.benchmark
# mkdir chappie.benchmark/chappie.dacapo.NOP
mkdir chappie.benchmark.VM_SAMPLE

benchmarks=(
avrora
# batik
# eclipse
# fop
# h2
# jython
# luindex
# lusearch
# pmd
# sunflow
# tomcat
# tradebeans
# tradesoap
# xalan
)

iter=0
export ITERS=1
export POLLING=4
export CORE_RATE=1
export JRAPL_RATE=1
export READ_JIFFIES=true

export MODE=VM_SAMPLE
for benchmark in "${benchmarks[@]}"; do
  # mkdir chappie.dacapo/${benchmark}
  for i in $(seq 0 $iter); do
    ./run.sh ../../benchmark_jars/dacapo-9.12-bach.jar "" Harness "-no-validation ${benchmark}"
    mkdir $benchmark
    mv chappie.dacapo-9.12-bach/* $benchmark

    for file in ${benchmark}/*.*.0.csv; do
      mv $file ${file%.0.csv}.$i.csv
    done

    mv ${benchmark}/log.hpl ${benchmark}/log.${i}.hpl

    mkdir chappie.benchmark.VM_SAMPLE/${benchmark}
    mv $benchmark/* chappie.benchmark.VM_SAMPLE/${benchmark}/.
    rm -rf $benchmark
  done
done

# export MODE=VM_SAMPLE
# for benchmark in "${benchmarks[@]}"; do
#   mkdir chappie.dacapo/${benchmark}
#   for i in $(seq 0 $iter); do
#     ../chappie/run/run.sh dacapo-9.12-bach.jar "" Harness "-no-validation ${benchmark}"
#     mkdir $benchmark
#     mv chappie.dacapo-9.12-bach/* $benchmark
#
#     for file in ${benchmark}/*.*.0.csv; do
#       mv $file ${file%.0.csv}.$i.csv
#     done
#
#     mv ${benchmark}/log.hpl ${benchmark}/log.${i}.hpl
#
#     mkdir chappie.benchmark/chappie.dacapo.SAMPLE/${benchmark}
#     mv $benchmark/* chappie.benchmark/chappie.dacapo.SAMPLE/${benchmark}/.
#     rm -rf $benchmark
#   done
# done

# rm -rf chappie.dacapo
# mkdir chappie.benchmark/chappie.dacapo
# mv chappie.benchmark/chappie.*.* chappie.benchmark/chappie.dacapo

# rm scratch -rf

# python3 runtime.py chappie.benchmark/chappie.dacapo

# mkdir chappie.dacapo
# cp -rf chappie.benchmark/chappie.dacapo/chappie.dacapo.SAMPLE chappie.dacapo

# sudo ./analyze_dacapo.sh new
