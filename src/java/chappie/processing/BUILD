load("@rules_java//java:defs.bzl", "java_binary", "java_library")
load("@dagger//:workspace_defs.bzl", "dagger_rules")

dagger_rules()

java_library(
  name = "attribution",
  visibility = ["//visibility:public"],
  srcs = ["AttributionModule.java"],
  deps = [
    ":dagger",
    "//src/java/chappie/attribution",
    "//src/java/chappie/attribution:sampler",
    "//src/java/chappie/profiling:core",
  ],
)
