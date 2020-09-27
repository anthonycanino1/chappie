load("@rules_java//java:defs.bzl", "java_library")
load("@dagger//:workspace_defs.bzl", "dagger_rules")

dagger_rules()

java_library(
  name = "module",
  visibility = ["//visibility:public"],
  srcs = glob(["*.java"]),
  deps = [
    "@jRAPL",
    ":dagger",
    "//src/java/chappie/attribution",
    "//src/java/chappie/attribution:sampler",
    "//src/java/chappie/profiling:core",
    "//src/java/chappie/profiling:sampling_rate",
    "//src/java/chappie/sampling/energy",
  ]
)
