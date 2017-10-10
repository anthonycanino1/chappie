#include <stdio.h>
#include "CPUScaler.h"
#include "jni.h"

int main(int argc, char **argv) {
	// Create a vm for inspection
	
	JavaVM* jvm;
	JNIEnv* env;
	JavaVMInitArgs args;
					
	args.version = JNI_VERSION_1_8;
	args.nOptions = 0;
	args.ignoreUnrecognized = JNI_FALSE;

	//JNI_CreateJavaVM(&jvm, (void **)&env, &args);

	jclass clazz;

	Java_jrapl_EnergyCheckUtils_ProfileInit(env, clazz);
	Java_jrapl_EnergyCheckUtils_EnergyStatCheck(env, clazz);
	Java_jrapl_EnergyCheckUtils_ProfileDealloc(env, clazz);

	return 0;
}
