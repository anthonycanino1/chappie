#!/bin/bash

# expected inputs:
# ./oltp.sh [benchmark] -d <directory>

CHAPPIE_PATH=/home/timur/Projects/chappie

JARS=${CHAPPIE_PATH}/chappie.jar:${CHAPPIE_PATH}/vendor/oltp/oltp.jar
JAVA9_PATH=/home/timur/Projects/dev/build/linux-x86_64-normal-server-release/jdk/bin/java

benchmark=$1
echo "=================================================="
echo "Starting oltp: $benchmark"
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
  $JAVA9_PATH -cp $JARS --add-modules java.xml.bind -Dlog4j.configuration=log4j.properties \
    chappie.Main file:/${CHAPPIE_PATH}/vendor/oltp/oltp.jar "com.oltpbenchmark.DBWorkload" \
    "-b $benchmark --execute=true -s 5 -c $CHAPPIE_PATH/vendor/oltp/config/sample_${benchmark}_config.xml"
else
  $JAVA9_PATH -cp $JARS -agentpath:$HP_PATH=interval=4,logPath=$HP_LOG            \
    --add-modules java.xml.bind -Dlog4j.configuration=log4j.properties            \
    chappie.Main file:/${CHAPPIE_PATH}/vendor/oltp/oltp.jar "com.oltpbenchmark.DBWorkload" \
    "-b $benchmark --execute=true -s 5 -c $CHAPPIE_PATH/vendor/oltp/config/sample_${benchmark}_config.xml"
fi
