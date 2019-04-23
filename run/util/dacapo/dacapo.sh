#!/bin/bash

# expected inputs:
# ./dacapo.sh [benchmark] -d <directory>
# echo $CHAPPIE_PATH
JARS="$CHAPPIE_PATH/chappie.jar:$CHAPPIE_PATH/util/chappie_callback.jar:$CHAPPIE_PATH/vendor/dacapo-9.12-MR1-bach.jar"
# echo $JARS
JAVA9_PATH=/home/timur/Projects/dev/build/linux-x86_64-normal-server-release/jdk/bin/java
# JAVA9_PATH=/home/timur/Projects/java9/java
# echo "Number of Iterations $ITERATIONS"

echo "=================================================="
echo "Starting dacapo: $1"
echo "=================================================="

directory=$3
rm -rf $directory/scratch
mkdir -p $directory

echo $directory
export CHAPPIE_DIRECTORY=$directory

HP_PATH=$CHAPPIE_PATH/src/async/build/liblagent.so
HP_LOG=$CHAPPIE_DIRECTORY/chappie.stack.csv

if [ $MODE == NOP ]; then
  $JAVA9_PATH -cp $JARS Harness $1 -s $2                              \
    --iterations $no_iterations --no-validation --scratch-directory $3/scratch         \
    --callback chappie.ChappieCallback
else
  $JAVA9_PATH -cp $JARS -agentpath:$HP_PATH=interval=${HP_POLLING},logPath=$HP_LOG \
    Harness $1 -s $2                                                    \
    --iterations $no_iterations --scratch-directory $3/scratch                         \
    --callback chappie.ChappieCallback
fi
rm -rf $directory/scratch
