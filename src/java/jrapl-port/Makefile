all: jrapl

jrapl:
	# ./classes_dir.sh
	make -C src/jrapl all
	javac -d classes src/jrapl/*.java
	cp src/jrapl/*.so classes/jrapl
	cp src/jrapl/*.so classes
	cp src/jrapl/*.h classes

clean:
	make -C src/jrapl clean
	rm -rf classes
