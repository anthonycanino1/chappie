#include <stdio.h>
#include <string.h>

#include <string>
#include <jvmti.h>

#include <assert.h>
#include <dlfcn.h>
#include <jvmti.h>
#include <jni.h>
#include <stdint.h>
#include <signal.h>
#include <time.h>
#include <string>



(*fptr)(void*, int, void*) asgct_;
#define inline *(void **)(&asgct_) AsyncGetCallTrace;

JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *jvm, char *options, void *reserved) {
    // int err;
    // jvmtiEnv *jvmti;

    void* i;
    void* k;
    int j;

    // int (*fptr)(void*, int, void*);
    AsyncGetCallTrace = dlsym(RTLD_DEFAULT, "AsyncGetCallTrace");
    // *(void **)(&AsyncGetCallTrace)
    (*AsyncGetCallTrace)(i, j, k);

    // char* buffer[50];
    // int n;
    // n = sprintf(buffer, "%d", j);
    // printf("%s", buffer);
    printf("%s", (char*)k);

    // sprintf(&(char*)(i), "%s");
    // sprintf(j, "%d");
    // sprintf(&(char*)(k), "%s");

    // bit_cast<FunctionType>(dlsym(RTLD_DEFAULT, "AsyncGetCallTrace"));
    // cout << func();

    return 0;
}
