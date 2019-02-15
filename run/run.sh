#/bin/bash

# expected inputs:
# ./run.sh [target.jar] [main class] <args> <d_args>

# potential exports:
# CHAPPIE_DIRECTORY (jar level bootstrapper)
# CHAPPIE_SUFFIX (benchmark/grid search)
# CHAPPIE_EXTRA_JARS (any additional jars your program needs)

dir=`dirname "$0"`

# setup chappie (maybe needs a better definition)
# global binding to ./run/? chappie "installer"?
export CHAPPIE_PATH="$dir/../chappie.jar"

# parse out the jar path
jar_path=$1
jar_path=$(echo "$(cd "$(dirname "$jar_path")" && pwd)/$(basename "$jar_path")")

jar_name=${jar_path##*/}
jar_name=${jar_name%\.*}

jar_url=file:/$jar_path

# set aliases for java inputs
main_class=$2
args=$3
d_args=$4

# setup honest profiler (this still needs clean up)
hp_path=$dir/../src/async/build/liblagent.so
if [ -z $CHAPPIE_SUFFIX ]
  then
    hp_log_path="${CHAPPIE_DIRECTORY}/chappie.stack.csv"
  else
    hp_log_path="${CHAPPIE_DIRECTORY}/chappie.stack.${CHAPPIE_SUFFIX}.csv"
fi

export JARS="$CHAPPIE_PATH:$jar_path"

#:$EXTRA_JARS"

# export JARS="$CHAPPIE_PATH:$jar_path"

# extra_jars=$2
# export JARS="$JARS:$extra_jars"

# chappie.*.csv chappie.$jar_name -rf

# has to happen here because of honest profiler
mkdir -p $CHAPPIE_DIRECTORY

/home/timur/Projects/dev/build/linux-x86_64-normal-server-release/jdk/bin/java \
        -Xbootclasspath/a:$CHAPPIE_PATH                                        \
        -cp $CHAPPIE_PATH:$jar_url                                             \
        -javaagent:$CHAPPIE_PATH                                               \
        -agentpath:$hp_path=interval=4,logPath=$hp_log_path                    \
        chappie.Main $jar_url $main_class $args $d_args

# # ../../dev/build/linux-x86_64-normal-server-release/jdk/bin/java $5 -Xbootclasspath/a:$CHAPPIE_PATH:$7 -cp $JARS$7 $6 -javaagent:$CHAPPIE_PATH -agentpath:$dir/../src/async/build/liblagent.so=interval=4,logPath="${DIRECTORY}/chappie.stack${ITER}.csv" chappie.Chaperone9 $jar_url $3 $4
#
# # if [ $MODE == NOP ] || [ $MODE == NAIVE ]; then
# #   # java $5 -Xbootclasspath/p:$CHAPPIE_PATH:$7 -cp $JARS$7 $6 -javaagent:$CHAPPIE_PATH chappie.Chaperone $jar_url $3 $4
# #   ../../dev/build/linux-x86_64-normal-server-release/jdk/bin/java $5 -Xbootclasspath/a:$CHAPPIE_PATH:$7 -cp $JARS$7 $6 -javaagent:$CHAPPIE_PATH chappie.Chaperone9 $jar_url $3 $4
# # else
# #   # java $5 -Xbootclasspath/p:$CHAPPIE_PATH:$7 -cp $JARS$7 $6 -javaagent:$CHAPPIE_PATH -agentpath:$dir/../src/async/build/liblagent.so=interval=4,logPath=log.hpl chappie.Chaperone $jar_url $3 $4
# #   ../../dev/build/linux-x86_64-normal-server-release/jdk/bin/java $5 -Xbootclasspath/a:$CHAPPIE_PATH:$7 -cp $JARS$7 $6 -javaagent:$CHAPPIE_PATH -agentpath:$dir/../src/async/build/liblagent.so=interval=4,logPath=$DIRECTORY/chappie.stack.csv chappie.Chaperone9 $jar_url $3 $4
# # fi
#
# # echo 'Moving data to chappie.'${jar_name}
# # mkdir chappie.$jar_name
# # mv chappie.*.*.* chappie.$jar_name
# # mv log.hpl chappie.$jar_name/log.0.hpl
