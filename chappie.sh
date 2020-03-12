#!/bin/bash

chappie_root=$(realpath `dirname "$0"`)
sudo $(./scripts/create-chappie-call.sh "$@")
