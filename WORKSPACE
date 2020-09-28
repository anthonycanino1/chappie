load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

# dagger deps
RULES_JVM_EXTERNAL_TAG = "3.3"
RULES_JVM_EXTERNAL_SHA = "d85951a92c0908c80bd8551002d66cb23c3434409c814179c0ff026b53544dab"

http_archive(
    name = "rules_jvm_external",
    strip_prefix = "rules_jvm_external-%s" % RULES_JVM_EXTERNAL_TAG,
    sha256 = RULES_JVM_EXTERNAL_SHA,
    url = "https://github.com/bazelbuild/rules_jvm_external/archive/%s.zip" % RULES_JVM_EXTERNAL_TAG,
)

DAGGER_TAG = "2.28.1"
DAGGER_SHA = "9e69ab2f9a47e0f74e71fe49098bea908c528aa02fa0c5995334447b310d0cdd"

http_archive(
    name = "dagger",
    strip_prefix = "dagger-dagger-%s" % DAGGER_TAG,
    sha256 = DAGGER_SHA,
    urls = ["https://github.com/google/dagger/archive/dagger-%s.zip" % DAGGER_TAG],
)

load("@rules_jvm_external//:defs.bzl", "maven_install")
load("@dagger//:workspace_defs.bzl", "DAGGER_ARTIFACTS", "DAGGER_REPOSITORIES")

maven_install(
    artifacts = DAGGER_ARTIFACTS,
    repositories = DAGGER_REPOSITORIES,
)

load("@bazel_tools//tools/build_defs/repo:maven_rules.bzl", "maven_jar")

maven_jar(
    name = "net_java_dev_jna_jna",
    artifact = "net.java.dev.jna:jna:5.4.0",
    repository = "https://repo1.maven.org/maven2",
)

load("@bazel_tools//tools/build_defs/repo:git.bzl", "git_repository", "new_git_repository")

git_repository(
    name = "clerk",
    commit = "6455cb51742f55a36be2c8d48205dd72053047dc",
    shallow_since = "1600974126 -0600",
    remote = "https://github.com/timurbey/clerk.git",
)

git_repository(
    name = "jRAPL",
    commit = "8447264da36454bc4163935c4013280e165b0d49",
    shallow_since = "1600653593 -0600",
    remote = "https://github.com/timurbey/jRAPL.git",
)

git_repository(
    name = "eflect",
    commit = "a5b15e9d436ad8c6c7766f7d1262eae87d97b45b",
    shallow_since = "1600975337 -0600",
    remote = "https://github.com/timurbey/eflect.git",
)

new_local_repository(
    name = "asyncProfiler",
    path = "/home/timur/projects/async-profiler",
    build_file_content = """
load("@rules_java//java:defs.bzl", "java_import")

genrule(
  name = "async-profiler-lib",
  visibility = ["//visibility:public"],
  cmd = "cp /home/timur/projects/async-profiler/build/libasyncProfiler.so $@",
  outs = ["libasyncProfiler.so"],
)

java_import(
    name = "asyncProfiler",
    visibility = ["//visibility:public"],
    jars = [
      "build/async-profiler.jar"
    ],
)
"""
)
