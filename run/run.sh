#/bin/bash

dir=`dirname "$0"`

jar_path=$1
jar_path=$(echo "$(cd "$(dirname "$jar_path")" && pwd)/$(basename "$jar_path")")

jar_name=${jar_path##*/}
jar_name=${jar_name%\.*}

jar_url=file:/$jar_path

rm chappie.*.csv chappie.$jar_name -rf
export CHAPPIE_PATH="$dir/../chappie.jar"
export JARS="$CHAPPIE_PATH:$jar_path"

extra_jars=$2
export JARS="$JARS:$extra_jars"

if [ $MODE == NOP ] || [ $MODE == NAIVE ]; then
  java $5 -Xbootclasspath/p:$CHAPPIE_PATH:$7 -cp $JARS$7 $6 -javaagent:$CHAPPIE_PATH chappie.Chaperone $jar_url $3 $4
else
  java $5 -Xbootclasspath/p:$CHAPPIE_PATH:$7 -cp $JARS$7 $6 -javaagent:$CHAPPIE_PATH -agentpath:$dir/../src/async/build/liblagent.so=interval=4,logPath=log.hpl chappie.Chaperone $jar_url $3 $4
fi

echo 'Moving data to chappie.'${jar_name}
mkdir chappie.$jar_name
mv chappie.*.*.* chappie.$jar_name
mv chappie.online.csv chappie.$jar_name/chappie.online.0.csv
#mv log.hpl chappie.$jar_name/log.0.hpl
mv log.hpl chappie.$jar_name/log.hpl
