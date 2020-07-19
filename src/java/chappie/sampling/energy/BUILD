load("@rules_java//java:defs.bzl", "java_library")

java_library(
    name = "energy",
    visibility = ["//visibility:public"],
    srcs = glob(["*.java"]),
    deps = [
      "@jRAPL",
      "//src/java/chappie/profiling:core",
      "//src/java/chappie/profiling:samples",
      "//src/java/chappie/util",
    ],
)
