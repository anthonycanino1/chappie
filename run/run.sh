#/bin/bash

jar_path=$1
jar_path=$(echo "$(cd "$(dirname "$jar_path")" && pwd)/$(basename "$jar_path")")

jar_name=${jar_path##*/}
jar_name=${jar_name%\.*}

jar_url=file:/$jar_path

rm chappie.*.csv chappie.$jar_name -rf
export ITERS=1
export STACK_PRINT=1
export MEMORY=1
export CHAPPIE_PATH="/home/acanino1/Projects/chappie/chappie.jar"
export JARS="$CHAPPIE_PATH:$jar_path"

extra_jars=$2
export JARS="$JARS:$extra_jars"

##If method_stats.jar agent is be invoked ...  $5 will be the full -javaagent parameter
##The calling script knows the path of its own chappie installation and the path the the jar file of method_utils.jar

java $5 -Xbootclasspath/p:$CHAPPIE_PATH:$7 -cp $JARS$7 $6 -javaagent:$CHAPPIE_PATH chappie.Chaperone $jar_url $3 $4

echo "Moving Chappie Files ..."
echo 'Moving data to chappie.'${jar_name}
mkdir chappie.$jar_name
mv chappie.*.*.* chappie.$jar_name
