load("@rules_java//java:defs.bzl", "java_library")
load("@dagger//:workspace_defs.bzl", "dagger_rules")

dagger_rules()

java_library(
    name = "attribution",
    visibility = ["//visibility:public"],
    srcs = [
      "EnergyAttributer.java",
      "EnergyAttribution.java",
    ],
    deps = [
      "//src/java/chappie/profiling:core",
    ],
)

java_library(
    name = "sampler",
    visibility = ["//visibility:public"],
    srcs = ["AttributionSampler.java"],
    deps = [":dagger"],
)
