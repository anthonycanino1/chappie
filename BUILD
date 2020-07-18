load("@rules_java//java:defs.bzl", "java_binary", "java_library")
load("@dagger//:workspace_defs.bzl", "dagger_rules")

dagger_rules()

java_binary(
    name = "chappie",
    main_class = "chappie.Driver",
    srcs = glob(
      ["src/main/java/chappie/*.java"] +
      ["src/main/java/chappie/naive/*.java"] +
      ["src/main/java/chappie/processing/**/*.java"] +
      ["src/main/java/chappie/sampling/**/*.java"]
    ),
    deps = [
      ":common",
      "@jRAPL",
      "@async_profiler",
      ":dagger",
    ],
    data = ["@jRAPL//src/jrapl:libCPUScaler.so"],
    jvm_flags = ["-Djava.library.path=external/jRAPL/src/jrapl"],
)

java_library(
    name = "common",
    srcs = glob(
      ["src/main/java/chappie/attribution/*.java"] +
      ["src/main/java/chappie/concurrent/*.java"] +
      ["src/main/java/chappie/profiling/*.java"] +
      ["src/main/java/chappie/util/*.java"]
    ),
    deps = [
      ":dagger",
    ]
)
