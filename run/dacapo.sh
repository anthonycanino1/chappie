#!/bin/bash

rm -rf chappie.dacapo
mkdir chappie.dacapo

# benchmarks="avrora batik eclipse h2 jython luindex lusearch pmd sunflow tomcat tradebeans tradesoap xalan"
benchmarks="eclipse"

for benchmark in $benchmarks; do
  ./run.sh ../../chappiebench/dacapo/dacapo-9.12-bach.jar "" Harness $benchmark
  mkdir $benchmark
  mv chappie.dacapo-9.12-bach/*.csv $benchmark
  mv $benchmark chappie.dacapo/$benchmark
done

rm scratch -rf
