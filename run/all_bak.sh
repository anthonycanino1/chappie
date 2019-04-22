echo "Running Chappie Single Case - Dacapo"
benchmarks=(avrora.large h2.large jython.large sunflow.large tradebeans.large tradesoap.large xalan.large lusearch-fix.default luindex.default pmd.default)

coapps=(avrora.large_avrora.large jython.large_jython.large xalan.large_xalan.large avrora.large_jython.large avrora.large_sunflow.large jython.large_sunflow.large h2.large_tradebeans.large h2.large_tradesoap.large)

os=20
bench_index=0
dir=`dirname "$0"`
echo "Current Dir $dir"
export CHAPPIE_PATH=$dir/..
export CHAPPIE_JAR=$CHAPPIE_PATH/chappie.jar
for bench_size in "${benchmarks[@]}"
do	
  		bench="${bench_size%%.*}"; 
		size="${bench_size#*.}"
		export benchmark=$bench
		for vm in 1 2 4 8
		do	
			for hp in 1 2 4 8
			do
				export VM_POLLING=$vm
				export HP_POLLING=$hp
				export OS_POLLING=$os
				export DS=$size
				export MODE=NOP
				echo "Executing $bench with VM=$vm, OS=$os, HP=$hp, dataset=$size, NOP"
  				echo "$CHAPPIE_PATH/run/util/dacapo/single.sh"
  				export MODE=FULL
				echo "Executing $bench with VM=$vm, OS=$os, HP=$hp, dataset=$size, FULL"
  				echo "$CHAPPIE_PATH/run/util/dacapo/single.sh"
			done
		done
		vm=4
		hp=4
		export VM_POLLING=$vm
		export HP_POLLING=$hp
		for os in 4 8 20 40 80
		do
			export OS_POLLING=$os
			export MODE=NOP
			echo "Executing $bench with VM=$vm, OS=$os, HP=$hp, dataset=$size, NOP"
  			echo "$CHAPPIE_PATH/run/util/dacapo/single.sh"
  			export MODE=FULL
			echo "Executing $bench with VM=$vm, OS=$os, HP=$hp, dataset=$size, FULL"
  			echo "$CHAPPIE_PATH/run/util/dacapo/single.sh"
		done
		echo "Benchmark Done ... Moving to Next One ..."
		echo "++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++"
done
os=40
vm=4
hp=4
echo "Running Chappie CoApps"
for coapp_bench_size in "${benchmarks[@]}"
do
	first_bench_size="${coapp_bench_size%%_*}";
	second_bench_size="${bench_size#*_}"
	first_bench="${first_bench_size%%.*}"
	second_bench="${second_bench_size%%.*}"
	first_size="${first_bench_size#*.}"
	second_size="${second_bench_size#*.}"
	echo "Running $first_bench $first_size with $second_bench $second_size"
	echo "VM=$vm, OS=$os, HP=$hp"
done



