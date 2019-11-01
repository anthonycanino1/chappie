#!/bin/bash

chappie_root=$(realpath `dirname "$0"`)

# LD_PRELOAD=/usr/local/lib/libjemalloc.so.2 MALLOC_CONF=prof_leak:true,lg_prof_sample:0,prof_final:true $(python3 $chappie_root/scripts/run "$@")
# $(python3 $chappie_root/scripts/run "$@")
# python3 $chappie_root/scripts/analysis "$@"

# $(python3 $chappie_root/scripts/run -cfg cases/h2.json -dir ../chappie-data/baseline/h2/1-4)
# $(python3 $chappie_root/scripts/run -cfg cases/jme.json -dir ../chappie-data/baseline/jme/1-4)
# $(python3 $chappie_root/scripts/run -cfg cases/pmd.json -dir ../chappie-data/baseline/pmd/1-4)

# $(python3 $chappie_root/scripts/run -cfg cases/h2.json -dir ../chappie-data/scalability/coapp-5/h2) &
# $(python3 $chappie_root/scripts/run -cfg cases/jme.json -dir ../chappie-data/scalability/coapp-5/jme) &
# $(python3 $chappie_root/scripts/run -cfg cases/pmd.json -dir ../chappie-data/scalability/coapp-5/pmd) &
# $(python3 $chappie_root/scripts/run -cfg cases/sunflow.json -dir ../chappie-data/scalability/coapp-5/sunflow) &
# $(python3 $chappie_root/scripts/run -cfg cases/lusearch.json -dir ../chappie-data/scalability/coapp-5/lusearch) &

$(python3 $chappie_root/scripts/run -cfg cases/h2.json -dir ../chappie-data/scalability/sunflow_h2/h2_1) &
$(python3 $chappie_root/scripts/run -cfg cases/h2.json -dir ../chappie-data/scalability/sunflow_h2/h2_2) &
$(python3 $chappie_root/scripts/run -cfg cases/sunflow.json -dir ../chappie-data/scalability/sunflow_h2/sunflow_1) &
$(python3 $chappie_root/scripts/run -cfg cases/sunflow.json -dir ../chappie-data/scalability/sunflow_h2/sunflow_2) &

wait $!

# python3 $chappie_root/scripts/analysis -dir ../chappie-data/baseline/h2/1-4 &
# python3 $chappie_root/scripts/analysis -dir ../chappie-data/baseline/jme/1-4 &
# python3 $chappie_root/scripts/analysis -dir ../chappie-data/baseline/pmd/1-4 &
#
# python3 $chappie_root/scripts/analysis -dir ../chappie-data/scalability/coapp-3/h2 &
# python3 $chappie_root/scripts/analysis -dir ../chappie-data/scalability/coapp-3/jme &
# python3 $chappie_root/scripts/analysis -dir ../chappie-data/scalability/coapp-3/pmd &

# python3 $chappie_root/scripts/analysis -dir ../chappie-data/scalability/coapp-5/h2 &
# python3 $chappie_root/scripts/analysis -dir ../chappie-data/scalability/coapp-5/jme &
# python3 $chappie_root/scripts/analysis -dir ../chappie-data/scalability/coapp-5/pmd &
# python3 $chappie_root/scripts/analysis -dir ../chappie-data/scalability/coapp-5/sunflow &
# python3 $chappie_root/scripts/analysis -dir ../chappie-data/scalability/coapp-5/lusearch &

# wait $!

# for i in {0..9}; do
#   $(python3 $chappie_root/scripts/run "$@" -dir ../chappie-data/scalability/default_h2-10/${i}) &
# done
#
# wait $!
#
# for i in {0..2}; do
#   python3 $chappie_root/scripts/analysis "$@" -dir ../chappie-data/scalability/default_h2-3/${i} &
# done
#
# for i in {0..4}; do
#   python3 $chappie_root/scripts/analysis "$@" -dir ../chappie-data/scalability/default_h2-5/${i} &
# done
#
# for i in {0..9}; do
#   python3 $chappie_root/scripts/analysis "$@" -dir ../chappie-data/scalability/default_h2-10/${i} &
# done
#
# wait $!
