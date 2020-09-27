load("@rules_java//java:defs.bzl", "java_library")
load("@dagger//:workspace_defs.bzl", "dagger_rules")

dagger_rules()

java_library(
    name = "concurrent",
    visibility = ["//visibility:public"],
    srcs = ["SelfScheduledRunnable.java"],
)

java_library(
    name = "module",
    visibility = ["//visibility:public"],
    srcs = ["ConcurrentModule.java"],
    deps = [
      ":dagger",
      "//src/java/chappie/profiling:core",
    ],
)
