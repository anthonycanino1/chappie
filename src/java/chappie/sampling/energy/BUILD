load("@rules_java//java:defs.bzl", "java_library")

java_library(
    name = "energy",
    visibility = ["//visibility:public"],
    srcs = glob(["*.java"]),
    deps = [
      "@jRAPL",
      "//src/main/java/chappie/profiling:core",
      "//src/main/java/chappie/profiling:samples",
      "//src/main/java/chappie/profiling:sampling_rate",
      "//src/main/java/chappie/util",
    ],
)
