load("@rules_java//java:defs.bzl", "java_library")
load("@dagger//:workspace_defs.bzl", "dagger_rules")

dagger_rules()

java_library(
    name = "core",
    visibility = ["//visibility:public"],
    srcs = [
      "Profile.java",
      "Sample.java",
      "SampleProcessor.java",
      "Sampler.java",
    ],
)

java_library(
    name = "samples",
    visibility = ["//visibility:public"],
    srcs = [
      "MergableSample.java",
      "TimestampedSample.java",
      "SampleSet.java"
    ],
    deps = [":core"],
)

java_library(
    name = "sampling_rate",
    visibility = ["//visibility:public"],
    srcs = ["SamplingRate.java"],
    deps = [":dagger"],
)

java_library(
    name = "profiler",
    visibility = ["//visibility:public"],
    srcs = [
      "Profiler.java",
      "SamplingRate.java",
    ],
    deps = [
      ":core",
      ":dagger",
      ":sampling_rate",
      "//src/main/java/chappie/concurrent",
      "//src/main/java/chappie/util",
    ],
)
