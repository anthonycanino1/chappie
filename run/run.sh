#/bin/bash

jar_path=$1
jar_path=$(echo "$(cd "$(dirname "$jar_path")" && pwd)/$(basename "$jar_path")")

jar_name=${jar_path##*/}
jar_name=${jar_name%\.*}

jar_url=file:/$jar_path

rm chappie.*.csv chappie.$jar_name -rf

export CHAPPIE_PATH="/home/timur/Projects/chappie/chappie.jar"
export JARS="$CHAPPIE_PATH:$jar_path"

extra_jars=$2
export JARS="$JARS:$extra_jars"

java -Xbootclasspath/p:$CHAPPIE_PATH -cp $JARS -javaagent:$CHAPPIE_PATH chappie.Chaperone $jar_url $3 $4

echo 'Moving data to chappie.'${jar_name}
mkdir chappie.$jar_name
mv chappie.*.*.* chappie.$jar_name
mv method_stats.*.* chappie.$jar_name
