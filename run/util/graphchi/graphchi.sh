#!/bin/bash

# expected inputs:
# ./oltp.sh [benchmark] -d <directory>

CHAPPIE_PATH=/home/timur/Projects/chappie

JARS=${CHAPPIE_PATH}/chappie.jar:${CHAPPIE_PATH}/vendor/graphchi/graphchi.jar
JAVA9_PATH=/home/timur/Projects/dev/build/linux-x86_64-normal-server-release/jdk/bin/java

benchmark=$1
echo "=================================================="
echo "Starting graphchi: $benchmark"
echo "=================================================="

directory=$benchmark
case $2 in
  -d) directory=$3;;
esac

export CHAPPIE_DIRECTORY=$directory

HP_PATH=$CHAPPIE_PATH/src/async/build/liblagent.so
HP_LOG=$CHAPPIE_DIRECTORY/chappie.stack.$CHAPPIE_SUFFIX.csv

echo $MODE
if [ $MODE == NOP ]; then
  $JAVA9_PATH -cp $JARS chappie.Main file:/${CHAPPIE_PATH}/vendor/graphchi/graphchi.jar \
  "edu.cmu.graphchi.apps.$benchmark" "/home/rsaxena3/work/graphchi-java/twitter/twitter-2010.txt 5"
else
  $JAVA9_PATH -cp $JARS -agentpath:$HP_PATH=interval=${HP_POLLING},logPath=$HP_LOG \
  chappie.Main file:/${CHAPPIE_PATH}/vendor/graphchi/graphchi.jar \
  "edu.cmu.graphchi.apps.$benchmark" "/home/rsaxena3/work/graphchi-java/twitter/twitter-2010.txt 5"
fi
