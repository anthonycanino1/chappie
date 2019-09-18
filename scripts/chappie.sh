java                                                                         \
  -Xbootclasspath/a:chappie.jar                                              \
  -agentpath:build/liblagent.so=interval=8,logPath=data/method.csv           \
  -javaagent:chappie.jar                                                     \
  -cp chappie.jar                                                            \
  chappie.Driver
