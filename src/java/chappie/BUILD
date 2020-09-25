load("@rules_java//java:defs.bzl", "java_binary", "java_library")
load("@dagger//:workspace_defs.bzl", "dagger_rules")

dagger_rules()

java_library(
  name = "chappie",
  srcs = [
    "Chappie.java",
    "CalmnessModule.java",
    "FrequencyHistogram.java"
  ],
  visibility = ["//visibility:public"],
  deps = [
    "@clerk//clerk",
    "@clerk//clerk:profiler",
    "@clerk//clerk/concurrent",
    "@clerk//clerk/concurrent:periodic_sampling",
    ":dagger",
  ],
)

java_binary(
  name = "calm_it",
  visibility = ["//visibility:public"],
  main_class = "chappie.Chappie",
  runtime_deps = [
    ":chappie",
  ],
)
