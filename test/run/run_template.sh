#!/bin/bash

rm *.csv -rf

# reconfigure to chappie path
export CHAPPIE_PATH="/home/<user name>/Projects/chappie/chappie.jar"
export JARS="$CHAPPIE_PATH:<jar path>"

jar_url=file://<jar path>

java -cp $JARS -javaagent:$CHAPPIE_PATH chappie.Chaperone $jar_url <main method> <args>

# echo 'parsing results'
./parse.py

mkdir chappie_data
mv *.csv chappie_data
