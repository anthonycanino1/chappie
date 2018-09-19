#include <jni.h>
#include <jvmti.h>
#include "stack_top.h"

static JavaVM *jvm = NULL;
static jvmtiEnv *jvmti = NULL;

JNIEXPORT jstring JNICALL Java_chappie_util_GLIBC_getStackTraceTop(JNIEnv* env, jobject o, jthread thread) {
  if (jvm == NULL && jvmti == NULL) {
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

  err = (*jvmti)->GetStackTrace(jvmti, thread, 0, 5, frames, &count);
  //printf("%i\n", count);

  char *err_name;
  err_name = NULL;
  (void)(*jvmti)->GetErrorName(jvmti, err, &err_name);
  //printf("ErrorCode:%d ErrorName:%s\n", err, err_name);

  if (err == JVMTI_ERROR_NONE && count >= 1) {
    char *name_ptr;
    char *sig_ptr;
    char *generic_ptr;
    err = (*jvmti)->GetMethodName(jvmti, frames[0].method, &name_ptr, &sig_ptr, &generic_ptr);
    return (*env)->NewStringUTF(env, name_ptr);
  } else {
    return (*env)->NewStringUTF(env, "");
  }
}
