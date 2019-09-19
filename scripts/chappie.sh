#!/bin/bash

chappie_root=$(realpath `dirname "$0"`)/..

# python3 $chappie_root/run -dargs chappie.os=4 chappie.vm=1 -cp chappie.jar chappie.Driver
# $(python3 $chappie_root/run -dargs chappie.os=4 chappie.vm=1 -cp chappie.jar chappie.Driver)
$(python3 $chappie_root/run "$@")

# java                                                                         \
#   -Xbootclasspath/a:chappie.jar                                              \
#   -agentpath:build/liblagent.so=interval=8,logPath=data/method.csv           \
#   -javaagent:chappie.jar                                                     \
#   -cp chappie.jar                                                            \
#   chappie.Driver
