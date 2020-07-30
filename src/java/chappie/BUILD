load("@rules_java//java:defs.bzl", "java_binary", "java_library")
load("@dagger//:workspace_defs.bzl", "dagger_rules")

dagger_rules()

java_library(
  name = "naive",
  srcs = ["Chappie.java"],
  visibility = ["//visibility:public"],
  deps = [
    ":dagger",
    "//src/java/chappie/concurrent:module",
    "//src/java/chappie/naive:module",
    # "//src/java/chappie/processing:attribution",
    "//src/java/chappie/processing:alignment",
    "//src/java/chappie/processing/trace",
    "//src/java/chappie/profiling:core",
    "//src/java/chappie/profiling:profiler",
  ],
)

java_binary(
  name = "driver",
  main_class = "chappie.Driver",
  srcs = ["Driver.java"],
  deps = [
    ":naive",
    "//src/java/chappie/profiling:core",
  ],
  resources = ["@jRAPL//src/jrapl:libCPUScaler.so"],
  jvm_flags = ["-Djava.library.path=/home/timur/projects/async-profiler/build"]
)
