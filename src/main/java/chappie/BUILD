load("@rules_java//java:defs.bzl", "java_binary", "java_library")
load("@dagger//:workspace_defs.bzl", "dagger_rules")

dagger_rules()

java_library(
  name = "chappie",
  srcs = ["Chappie.java"],
  deps = [
    ":dagger",
    "//src/main/java/chappie/attribution",
    "//src/main/java/chappie/naive:module",
    "//src/main/java/chappie/profiling:core",
    "//src/main/java/chappie/profiling:profiler",
  ],
)

java_binary(
  name = "driver",
  main_class = "chappie.Driver",
  srcs = ["Driver.java"],
  deps = [
    ":chappie",
    "//src/main/java/chappie/profiling:core",
  ],
  data = ["@jRAPL//src/jrapl:libCPUScaler.so"],
  jvm_flags = ["-Djava.library.path=external/jRAPL/src/jrapl"],
)
