load("@rules_java//java:defs.bzl", "java_binary")
load("@dagger//:workspace_defs.bzl", "dagger_rules")

dagger_rules()

java_binary(
  name = "dacapo",
  visibility = ["//visibility:public"],
  main_class = "Harness",
  srcs = [
    "DaCapo.java",
  ],
  deps = [
    "//profiler/chappie:rank_it",
    "@clerk//clerk:profiler",
    "@dacapo",
    "@eflect//profiler/eflect",
    "//profiler/chappie:ranking_module",
    "@clerk//clerk/concurrent:periodic_sampling",
  ],
  jvm_flags = ["-Dchappie.output=/home/timur/sandbox/chappie/rankings"],
)
