#include <jni.h>
#include <jvmti.h>
#include "stack_top.h"

static JavaVM *jvm = NULL;
static jvmtiEnv *jvmti = NULL;

JNIEXPORT jstring JNICALL Java_chappie_util_GLIBC_getStackTraceTop(JNIEnv* env, jobject o, jthread thread) {
  if (jvm == NULL && jvmti == NULL) {
    printf("ONE TIME INIT\n");
    int ec = (*env)->GetJavaVM(env, &jvm);
    if (ec != 0) {
      printf("Failed to get JavaVM!\n");
    }
    ec = (*jvm)->GetEnv(jvm, &jvmti, JVMTI_VERSION_1_0);
    if (ec != 0) {
      printf("Failed to get JVM TI Environment!\n");
    }
  }

  jvmtiFrameInfo frames[5];
  jint count = 0;
  jvmtiError err = JVMTI_ERROR_NONE;

  err = (*jvmti)->PeekStackTrace(jvmti, thread, 0, 1, frames, &count);
  //err = (*jvmti)->GetStackTrace(jvmti, thread, 0, 1, frames, &count);

  if (err == JVMTI_ERROR_NONE && count >= 1) {
    char *name_ptr;
    char *sig_ptr;
    char *generic_ptr;
    err = (*jvmti)->GetMethodName(jvmti, frames[0].method, &name_ptr, &sig_ptr, &generic_ptr);

    jclass clazz;
    err = (*jvmti)->GetMethodDeclaringClass(jvmti, frames[0].method, &clazz);

    err = (*jvmti)->GetClassSignature(jvmti, clazz, &sig_ptr, &generic_ptr);

    char *p = sig_ptr;
    while (*p != 0) {
      if (*p == '/' || *p == ';') *p = '.';
      ++p;
    }

    char fqn[256];
    sprintf(fqn, "%s%s", sig_ptr+1, name_ptr);

    return (*env)->NewStringUTF(env, fqn);
  } else {
    return (*env)->NewStringUTF(env, "NULL");
  }
}
