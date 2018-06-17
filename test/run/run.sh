#!/bin/bash

rm *.csv -rf

# reconfigure to chappie path
export CHAPPIE_PATH="/home/timur/Projects/chappie/chappie.jar"
export JARS="$CHAPPIE_PATH:../chappie_test.jar"

jar_url=file://home/timur/Projects/chappie/test/chappie_test.jar

java -cp $JARS -javaagent:$CHAPPIE_PATH chappie.Chaperone $jar_url chappie_test.Benchmark 1

# echo 'parsing results'
./parse.py

mkdir chappie_logs
mv *.csv chappie_logs
