java -Xbootclasspath/a:chappie.jar -agentpath:src/async/build/liblagent.so=interval=4,logPath=data/method.csv -javaagent:chappie.jar -cp chappie.jar chappie.Driver
