load("@rules_java//java:defs.bzl", "java_library")

java_library(
    name = "trace",
    visibility = ["//visibility:public"],
    srcs = glob(["StackTrace*.java"]),
    deps = [
      "@async_profiler",
      "//src/main/java/chappie/profiling:core",
      "//src/main/java/chappie/profiling:samples",
      "//src/main/java/chappie/util",
    ],
)
