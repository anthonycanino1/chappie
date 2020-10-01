#!/bin/bash

function run_benchmarks {
  for benchmark in ${benchmarks[@]}; do
    experiment_dir=$data_dir/${benchmark}-${size}
    # java -Dchappie.output=$experiment_dir -jar $jar_file $benchmark -c chappie.experiments.DaCapo -s $size -n $iterations
    bazel run experiments/chappie/experiments:dacapo --host_jvm_args="-Dchappie.out=${experiment_dir}" $benchmark -c chappie.experiments.DaCapo -s $size -n $iterations
  done
}

jar_file=bazel-bin/experiments/chappie/experiments/dacapo_deploy.jar
data_dir=/home/timur/sandbox/chappie/data
iterations=20

size=large
benchmarks=(
  avrora
  batik
  h2
  sunflow
)
run_benchmarks

size=huge
benchmarks=(
  batik
  graphchi
)
run_benchmarks
