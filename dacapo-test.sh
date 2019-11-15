#!/bin/bash

chappie_root=$(realpath `dirname "$0"`)

# ./dacapo.sh batik 10 large
# ./dacapo.sh biojava 10 default
# ./dacapo.sh eclipse 10 large
# ./dacapo.sh fop 100 default
# ./dacapo.sh graphchi 3 huge
# ./dacapo.sh h2 10 large
# ./dacapo.sh jme 100 default
# ./dacapo.sh jython 10 default
# ./dacapo.sh kafka 100 default
# ./dacapo.sh luindex 3 huge
# ./dacapo.sh lusearch 10 huge
# ./dacapo.sh pmd 10 large
# ./dacapo.sh sunflow 10 large
# ./dacapo.sh tomcat 10 large
# ./dacapo.sh xalan 10 default

for bench in ../chappie-data/stop/*; do
  python3 ${chappie_root}/scripts/analysis -d $bench &
done

wait $!
