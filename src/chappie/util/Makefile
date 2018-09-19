TARGET = lib_stack_top.so stack_top.o
CFLAGS = -fPIC -g -c -Wall
JAVA_INCLUDE = $(JAVA_HOME)/include
JAVA_INCLUDE_LINUX = $(JAVA_INCLUDE)/linux

all: lib_shared_stack_top

common:
	echo $(JAVA_HOME)
	gcc $(CFLAGS) -I $(JAVA_INCLUDE) -I$(JAVA_INCLUDE_LINUX) stack_top.c

lib_shared_stack_top: common
	gcc -g -Wall -I $(JAVA_INCLUDE) -I $(JAVA_INCLUDE_LINUX) -shared -Wl,-soname,lib_stack_top.so -o lib_stack_top.so stack_top.o
	
clean:
	rm -f lib_share_stack_top $(TARGET) cdriver
