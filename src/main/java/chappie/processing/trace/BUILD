load("@rules_java//java:defs.bzl", "java_library")
load("@dagger//:workspace_defs.bzl", "dagger_rules")

dagger_rules()

java_library(
  name = "trace",
  visibility = ["//visibility:public"],
  srcs = glob(["*.java"]),
  deps = [
    "@jRAPL",
    ":dagger",
    "//src/main/java/chappie/attribution",
    "//src/main/java/chappie/profiling:core",
    "//src/main/java/chappie/profiling:samples",
    "//src/main/java/chappie/sampling/trace",
    "//src/main/java/chappie/util",
  ],
)
