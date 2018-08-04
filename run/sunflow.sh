#!/bin/bash

export MODE=0
export ITERS=1

export TIMES=1
export STEPS=1

export SUNFLOW_WIDTH=320
export SUNFLOW_HEIGHT=240

./run.sh ../../chappiebench/sunflow/release/sunflow.jar ../../chappiebench/sunflow/janino.jar SunflowGUI "-rtbench -nogui -v 0 ../../chappiebench/sunflow/run/large.sc"
