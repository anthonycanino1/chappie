load("@rules_java//java:defs.bzl", "java_binary", "java_library")
load("@dagger//:workspace_defs.bzl", "dagger_rules")

dagger_rules()

java_library(
  name = "chappie",
  visibility = ["//visibility:public"],
  srcs = [
    "StackTraceRanking.java",
  ],
)

java_library(
  name = "calmness",
  srcs = [
    "CalmnessModule.java",
  ],
  visibility = ["//visibility:public"],
  deps = [
    "@clerk//clerk",
    ":dagger",
  ],
)

java_library(
  name = "alignment",
  srcs = [
    "AlignmentModule.java"
  ],
  visibility = ["//visibility:public"],
  deps = [
    "@clerk//clerk",
    ":dagger",
  ],
)

# java_library(
#   name = "chappie",
#   srcs = [
#     "Chappie.java",
#     "CalmnessModule.java",
#     "FrequencyHistogram.java"
#   ],
#   visibility = ["//visibility:public"],
#   deps = [
#     "@clerk//clerk",
#     "@clerk//clerk:profiler",
#     "@clerk//clerk/concurrent",
#     "@clerk//clerk/concurrent:periodic_sampling",
#     ":dagger",
#   ],
# )

java_binary(
  name = "calm_it",
  visibility = ["//visibility:public"],
  main_class = "chappie.CalmnessProfiler",
  srcs = [
    "CalmnessProfiler.java",
  ],
  deps = [
    ":calmness",
    "@clerk//clerk:profiler",
    "@clerk//clerk/concurrent:periodic_sampling",
    ":dagger"
  ],
  jvm_flags = ['-Dclerk.sampling.rate=512']
)
