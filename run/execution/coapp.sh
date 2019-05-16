#!/bin/bash

# expected inputs:
# ./dacapo.sh [program 1] [program 2] -d <directory>


$1 &
first_id=$!

$2 &
second_id=$!

tail --pid=${first_id} -f /dev/null
tail --pid=${second_id} -f /dev/null

