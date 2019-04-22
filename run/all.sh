echo "Running Chappie Single Case - Dacapo"
benchmarks=(avrora h2 jython sunflow tradebeans tradesoap xalan lusearch-fix luindex pmd)
sizes=(large large large large large large large default default default)
os=20
bench_index=0
dir=`dirname "$0"`
echo "Current Dir $dir"
export CHAPPIE_PATH=$dir/..
export CHAPPIE_JAR=$CHAPPIE_PATH/chappie.jar
for bench in "${benchmarks[@]}"
do
		export benchmark=$bench
		size="${sizes[$bench_index]}"
		bench_index=$((bench_index+1))
		for vm in 1 2 4 8
		do	
			for hp in 1 2 4 8
			do
				echo "Executing $bench with VM=$vm, OS=$os, HP=$hp, dataset=$size"
				export VM_POLLING=$vm
				export HP_POLLING=$hp
				export OS_POLLING=$os
				export DS=$size
  				export MODE=NOP
  				echo "$CHAPPIE_PATH/run/util/dacapo/single.sh"
  				export MODE=FULL
  				echo "$CHAPPIE_PATH/run/util/dacapo/single.sh"
			done
		done
done



