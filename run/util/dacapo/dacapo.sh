#!/bin/bash

# expected inputs:
# ./dacapo.sh [benchmark] -d <directory>

JARS="$CHAPPIE_PATH/chappie.jar:$CHAPPIE_PATH/util/chappie_callback.jar:$CHAPPIE_PATH/vendor/dacapo-9.12-MR1-bach.jar"
JAVA9_PATH=/home/timur/Projects/dev/build/linux-x86_64-normal-server-release/jdk/bin/java

benchmark=$1
echo "=================================================="
echo "Starting $benchmark"
echo "=================================================="

directory=$benchmark
case $2 in
  -d) directory=$3;;
esac

rm -rf $directory/scratch
mkdir -p $directory

export CHAPPIE_DIRECTORY=$directory

HP_PATH=$CHAPPIE_PATH/src/async/build/liblagent.so
HP_LOG=$CHAPPIE_DIRECTORY/chappie.stack.csv

if [ $MODE == NOP ]; then
  $JAVA9_PATH -cp $JARS Harness $benchmark -s small                               \
    --iterations 20 --no-validation --scratch-directory $directory/scratch        \
    --callback chappie.ChappieCallback
else
  $JAVA9_PATH -cp $JARS -agentpath:$HP_PATH=interval=4,logPath=$HP_LOG            \
    Harness $benchmark -s small                                                   \
    --iterations 20 --scratch-directory $directory/scratch                        \
    --callback chappie.ChappieCallback
fi
rm -rf $directory/scratch