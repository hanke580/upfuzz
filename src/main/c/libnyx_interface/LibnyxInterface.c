#include <jni.h>
#include "libnyx.h"

#define PTR_FIELD_NAME "nyx_process_ptr"


// get the pointer from the field of the class called PTR_FIELD_NAME
// note the field must be type long
NyxProcess* get_nyx_ptr(JNIEnv *env, jobject thisObj) {
    jclass myClass = (*env)->GetObjectClass(env, thisObj);
    jfieldID ptrField = (*env)->GetFieldID(env, myClass, PTR_FIELD_NAME, "J");
    return (NyxProcess*) (*env)->GetLongField(env, thisObj, ptrField);
}

// save the pointer in the field of the class called PTR_FIELD_NAME
// note the field must be type long
void set_nyx_ptr(JNIEnv *env, jobject thisObj, NyxProcess* nyx_runner) {
    jclass myClass = (*env)->GetObjectClass(env, thisObj);
    jfieldID ptrField = (*env)->GetFieldID(env, myClass, PTR_FIELD_NAME, "J");
    (*env)->SetLongField(env, thisObj, ptrField, (jlong) nyx_runner);
}

void throwErrorOrPrint(JNIEnv *env, jclass exception_class, const char* err_message) {
    if (exception_class == NULL) {
        printf("%s\n", err_message);
    } else {
        (*env)->ThrowNew(env, exception_class, err_message);
    }
}

/*
 * Class:     org_zlab_upfuzz_nyx_LibnyxInterface
 * Method:    nyxNew
 * Signature: (Ljava/lang/String;Ljava/lang/String;IIZ)V
 */
JNIEXPORT void JNICALL Java_org_zlab_upfuzz_nyx_LibnyxInterface_nyxNew(
    JNIEnv *env, jobject thisObj,
    jstring sharedDir, jstring workDir, jint cpuID, jint inputBufferSize, jboolean inputBufferWriteProtection
) {
    const char* shared_dir = (*env)->GetStringUTFChars(env, sharedDir, NULL);
    const char* work_dir = (*env)->GetStringUTFChars(env, workDir, NULL);
    const int32_t cpu_id = (int32_t) cpuID;
    const int32_t input_buf_size = (int32_t) inputBufferSize;
    const bool input_buf_write_protection = (bool) inputBufferWriteProtection;

    NyxProcess* nyx_runner = nyx_new(shared_dir, work_dir, cpu_id, input_buf_size, input_buf_write_protection);
    
    if (nyx_runner == NULL) {
        jclass exceptionClass = (*env)->FindClass(env, "java/lang/RuntimeException");
        throwErrorOrPrint(env, exceptionClass, "Error: nyx instance was null");
        return;
    }

    //save the pointer in the field of the class
    set_nyx_ptr(env, thisObj, nyx_runner);

    //give back the string conversions memory
    (*env)->ReleaseStringUTFChars(env, sharedDir, shared_dir);
    (*env)->ReleaseStringUTFChars(env, workDir, work_dir);
}

/*
 * Class:     org_zlab_upfuzz_nyx_LibnyxInterface
 * Method:    nyxShutdown
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_zlab_upfuzz_nyx_LibnyxInterface_nyxShutdown(
    JNIEnv *env, jobject thisObj
) {
    NyxProcess* nyx_runner = get_nyx_ptr(env, thisObj);

    if (nyx_runner == NULL) {
        jclass exceptionClass = (*env)->FindClass(env, "java/lang/RuntimeException");
        throwErrorOrPrint(env, exceptionClass, "Error: nyx instance was null");
        return;
    }

    nyx_shutdown(nyx_runner);
    set_nyx_ptr(env, thisObj, (NyxProcess*) 0 ); // set it to 0
}

/*
 * Class:     org_zlab_upfuzz_nyx_LibnyxInterface
 * Method:    nyxExec
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_zlab_upfuzz_nyx_LibnyxInterface_nyxExec(
    JNIEnv *env, jobject thisObj
) {
    NyxProcess* nyx_runner = get_nyx_ptr(env, thisObj);

    // Hardcoded no timeout
    nyx_option_set_timeout(nyx_runner, 0, 0);
    nyx_option_apply(nyx_runner);

    jclass exceptionClass = (*env)->FindClass(env, "java/lang/RuntimeException");
    if (nyx_runner == NULL) {
        throwErrorOrPrint(env, exceptionClass, "Error: nyx instance was null");
        return;
    }
        
    switch (nyx_exec(nyx_runner)) {
    case Normal:
        //printf("Normal!!!\n");
        break;
    case Abort:
        throwErrorOrPrint(env, exceptionClass, "Error: Nyx abort occured");
        break;
    case IoError:
        throwErrorOrPrint(env, exceptionClass, "Error: QEMU-Nyx has died");
        break;
    case Error:
        throwErrorOrPrint(env, exceptionClass, "Error: Nyx runtime error has occured");
        break;
    case Timeout:
        throwErrorOrPrint(env, exceptionClass, "Error: Nyx exec timed out");
        break;
    default:
        break;
  }
}

/*
 * Class:     org_zlab_upfuzz_nyx_LibnyxInterface
 * Method:    setInput
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_org_zlab_upfuzz_nyx_LibnyxInterface_setInput(
    JNIEnv *env, jobject thisObj, jstring input) {

    
    NyxProcess* nyx_runner = get_nyx_ptr(env, thisObj);

    if (nyx_runner == NULL) {
        jclass exceptionClass = (*env)->FindClass(env, "java/lang/RuntimeException");
        throwErrorOrPrint(env, exceptionClass, "Error: nyx instance was null");
        return;
    }

    const char* input_c = (*env)->GetStringUTFChars(env, input, NULL);
    const uint32_t input_length = (uint32_t) (*env)->GetStringLength(env, input) + 1;

    nyx_set_afl_input(nyx_runner, input_c, input_length);
    (*env)->ReleaseStringUTFChars(env, input, input_c);
}