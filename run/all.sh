echo "Running Chappie Single Case"
benchmarks=(avrora h2 jython sunflow tradebeans tradesoap xalan lusearch-fix luindex pmd)
sizes=(large large large large large large large default default default)
os=20
bench_index=0
for bench in "${benchmarks[@]}"
do
		size="${sizes[$bench_index]}"
		bench_index=$((bench_index+1))
		for vm in 1 2 4 8
		do	
			for hp in 1 2 4 8
			do
				echo "Executing $bench with VM=$vm, OS=$os, HP=$hp, dataset=$size"

			done
		done
done



