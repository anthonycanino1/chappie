#!/bin/bash


benchmark=benchmark/dacapo.json
echo $benchmark
$(python3 ./parser/run_benchmark.py -benchmark $benchmark -config config/NOP.xml)

# # python3 ./parser/run_benchmark.py -benchmark $benchmark -config config/NOP.xml
# # $(python3 ./parser/run_benchmark.py -benchmark $benchmark -config config/NOP.xml)
# # python3 ./parser/run_benchmark.py -benchmark $benchmark -config config/SAMPLE.xml
# $(python3 ./parser/run_benchmark.py -benchmark $benchmark -config config/SAMPLE.xml)

# python3 ./analysis/process.py -benchmark $benchmark -config config/NOP.xml
# python3 ./analysis/process.py -benchmark $benchmark -config config/SAMPLE.xml
# #
# # # benchmark=data/precision/sunflow
# python3 ./analysis/summary.py -benchmark graphchi
#
# # echo $benchmark
# #
# python3 ./analysis/method_plots.py -benchmark graphchi
#
# # root=data/precision/h2
# python3 ./analysis/heatmap.py -benchmark graphchi
# # python3 ./analysis/method_plots.py -benchmark $root
#
# # for benchmark in $root/*; do
# #   echo $benchmark
# #   python3 ./analysis/method_plots.py -benchmark $benchmark
# # done
