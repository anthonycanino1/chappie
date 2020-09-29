load("@rules_java//java:defs.bzl", "java_binary", "java_library")
load("@dagger//:workspace_defs.bzl", "dagger_rules")

dagger_rules()

java_library(
  name = "chappie",
  visibility = ["//visibility:public"],
  srcs = [
    "StackTraceRanker.java",
    # "StackTraceRanking.java",
  ],
  deps = [
    "//profiler/chappie/util",
    "@clerk//clerk",
    ":dagger",
    "@eflect//profiler/eflect",
    "@eflect//profiler/eflect/data",
  ]
)

java_library(
  name = "calmness_module",
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
  name = "ranking_module",
  srcs = [
    "StackTraceRankingModule.java",
  ],
  visibility = ["//visibility:public"],
  deps = [
    ":chappie",
    "@asyncProfiler",
    "@clerk//clerk",
    ":dagger",
    "@eflect//profiler/eflect/data",
  ],
)

java_binary(
  name = "calm_it",
  visibility = ["//visibility:public"],
  main_class = "chappie.CalmnessProfiler",
  srcs = [
    "CalmnessProfiler.java",
  ],
  deps = [
    ":calmness_module",
    "@clerk//clerk:profiler",
    "@clerk//clerk/concurrent:periodic_sampling",
    ":dagger"
  ],
  jvm_flags = ['-Dclerk.sampling.rate=512']
)

java_binary(
  name = "rank_it",
  visibility = ["//visibility:public"],
  main_class = "chappie.ChappieProfiler",
  srcs = [
    "ChappieProfiler.java",
  ],
  deps = [
    ":ranking_module",
    "@asyncProfiler",
    "@clerk//clerk:profiler",
    "@clerk//clerk/concurrent",
    "@clerk//clerk/concurrent:periodic_sampling",
    ":dagger",
    "@eflect//profiler/eflect",
  ],
  resources = [
    "@asyncProfiler//:async-profiler-lib",
    "@jRAPL//src/jrapl:libCPUScaler.so",
  ],
)
