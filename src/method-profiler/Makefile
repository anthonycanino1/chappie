make:
	gcc -c -I /usr/lib/jvm/java-11-openjdk-amd64/include -I /usr/lib/jvm/java-11-openjdk-amd64/include/linux agent.cpp
	gcc -I /usr/lib/jvm/java-11-openjdk-amd64/include -I /usr/lib/jvm/java-11-openjdk-amd64/include/linux -shared -Wl,-soname,agent.so -o agent.so agent.o -lc
