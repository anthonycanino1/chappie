load("@rules_java//java:defs.bzl", "java_library")
load("@dagger//:workspace_defs.bzl", "dagger_rules")

dagger_rules()

java_library(
  name = "trace",
  visibility = ["//visibility:public"],
  srcs = glob(["*.java"], exclude = ["StackTraceRanking.java"]),
  deps = [
    "@jRAPL",
    ":dagger",
    ":ranking",
    "//src/java/chappie/attribution",
    "//src/java/chappie/attribution:profile",
    "//src/java/chappie/profiling:core",
    "//src/java/chappie/profiling:samples",
    "//src/java/chappie/sampling/trace",
    "//src/java/chappie/util",
  ],
)

java_library(
  name = "ranking",
  visibility = ["//visibility:public"],
  srcs = ["StackTraceRanking.java"],
  deps = [
    "@jRAPL",
    ":dagger",
    "//src/java/chappie/sampling/trace",
    "//src/java/chappie/util",
  ],
)
