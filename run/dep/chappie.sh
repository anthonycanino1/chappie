# !/bin/bash

# for b in data/precision/*; do
#   b=$(basename $b)
#   echo $b
#   cp temp/$b/1_${b}/timerRate1_osFactor2 data/precision/$b/1_${b}/timerRate1_vmFactor1_osFactor2 -rf
# done

dir=$(realpath `dirname "$0"`)

# experiment=sleep.json
# experiment_dir=$(python3 ./parser/setup_experiment.py -experiment $experiment)

experiment=rate.json
experiment_dir=$(python3 ./parser/setup_experiment.py -experiment $experiment)

config_dir=$experiment_dir/config
benchmark_dir=$experiment_dir/benchmark

# for benchmark in $benchmark_dir/*; do
#   config=$dir/config/NOP.xml
#   $(python3 ./parser/run_benchmark.py -benchmark $benchmark -config $config)
#   # ./run_coapp.sh $benchmark_dir $config
# done
#
# for benchmark in $benchmark_dir/*; do
#   for config in $config_dir/*; do
#     $(python3 ./parser/run_benchmark.py -benchmark $benchmark -config $config)
#     # ./run_coapp.sh $benchmark_dir $config
#   done
# done

# config=$dir/config/NOP.xml
# echo 'NOP'
# for benchmark in $benchmark_dir/*; do
#   python3 ./analysis/process.py -benchmark $benchmark -config $config
# done
#
# for config in $config_dir/*; do
#   echo $config
#   for benchmark in $benchmark_dir/*; do
#     python3 ./analysis/process.py -benchmark $benchmark -config $config
#   done
# done

echo 'starting summary'
# for benchmark in $benchmark_dir/*; do
# # for benchmark in temp2/precision/*; do
  benchmark=data/precision/tpcc
  echo $benchmark
  # python3 ./analysis/summary.py -benchmark $benchmark
#   # echo 'plotting!'
# # #   # exit
  # python3 ./analysis/heatmap.py -benchmark $benchmark
  # python3 ./analysis/method_plots.py -benchmark $benchmark
# # done
#
# python3 ./analysis/attribution_plot.py -benchmark data/precision
python3 ./analysis/all_heatmap.py -benchmark data/precision
#
echo 'done!'
