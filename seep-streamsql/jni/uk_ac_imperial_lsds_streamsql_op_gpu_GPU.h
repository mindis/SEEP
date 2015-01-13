/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class uk_ac_imperial_lsds_streamsql_op_gpu_GPU */

#ifndef _Included_uk_ac_imperial_lsds_streamsql_op_gpu_GPU
#define _Included_uk_ac_imperial_lsds_streamsql_op_gpu_GPU
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     uk_ac_imperial_lsds_streamsql_op_gpu_GPU
 * Method:    getPlatform
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_uk_ac_imperial_lsds_streamsql_op_gpu_GPU_getPlatform
  (JNIEnv *, jobject);

/*
 * Class:     uk_ac_imperial_lsds_streamsql_op_gpu_GPU
 * Method:    getDevice
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_uk_ac_imperial_lsds_streamsql_op_gpu_GPU_getDevice
  (JNIEnv *, jobject);

/*
 * Class:     uk_ac_imperial_lsds_streamsql_op_gpu_GPU
 * Method:    createContext
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_uk_ac_imperial_lsds_streamsql_op_gpu_GPU_createContext
  (JNIEnv *, jobject);

/*
 * Class:     uk_ac_imperial_lsds_streamsql_op_gpu_GPU
 * Method:    createCommandQueue
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_uk_ac_imperial_lsds_streamsql_op_gpu_GPU_createCommandQueue
  (JNIEnv *, jobject);

/*
 * Class:     uk_ac_imperial_lsds_streamsql_op_gpu_GPU
 * Method:    createInputBuffer
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL Java_uk_ac_imperial_lsds_streamsql_op_gpu_GPU_createInputBuffer
  (JNIEnv *, jobject, jint);

/*
 * Class:     uk_ac_imperial_lsds_streamsql_op_gpu_GPU
 * Method:    createOutputBuffer
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL Java_uk_ac_imperial_lsds_streamsql_op_gpu_GPU_createOutputBuffer
  (JNIEnv *, jobject, jint);

/*
 * Class:     uk_ac_imperial_lsds_streamsql_op_gpu_GPU
 * Method:    createWindowStartPointersBuffer
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL Java_uk_ac_imperial_lsds_streamsql_op_gpu_GPU_createWindowStartPointersBuffer
  (JNIEnv *, jobject, jint);

/*
 * Class:     uk_ac_imperial_lsds_streamsql_op_gpu_GPU
 * Method:    createWindowEndPointersBuffer
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL Java_uk_ac_imperial_lsds_streamsql_op_gpu_GPU_createWindowEndPointersBuffer
  (JNIEnv *, jobject, jint);

/*
 * Class:     uk_ac_imperial_lsds_streamsql_op_gpu_GPU
 * Method:    createProgram
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_uk_ac_imperial_lsds_streamsql_op_gpu_GPU_createProgram
  (JNIEnv *, jobject, jstring);

/*
 * Class:     uk_ac_imperial_lsds_streamsql_op_gpu_GPU
 * Method:    createKernel
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_uk_ac_imperial_lsds_streamsql_op_gpu_GPU_createKernel
  (JNIEnv *, jobject, jstring);

/*
 * Class:     uk_ac_imperial_lsds_streamsql_op_gpu_GPU
 * Method:    setProjectionKernelArgs
 * Signature: (IIZ)I
 */
JNIEXPORT jint JNICALL Java_uk_ac_imperial_lsds_streamsql_op_gpu_GPU_setProjectionKernelArgs
  (JNIEnv *, jobject, jint, jint, jboolean);

/*
 * Class:     uk_ac_imperial_lsds_streamsql_op_gpu_GPU
 * Method:    setReductionKernelArgs
 * Signature: (IIZ)I
 */
JNIEXPORT jint JNICALL Java_uk_ac_imperial_lsds_streamsql_op_gpu_GPU_setReductionKernelArgs
  (JNIEnv *, jobject, jint, jint, jboolean);

/*
 * Class:     uk_ac_imperial_lsds_streamsql_op_gpu_GPU
 * Method:    invokeKernel
 * Signature: (IIZZ)I
 */
JNIEXPORT jint JNICALL Java_uk_ac_imperial_lsds_streamsql_op_gpu_GPU_invokeKernel
  (JNIEnv *, jobject, jint, jint, jboolean, jboolean);

/*
 * Class:     uk_ac_imperial_lsds_streamsql_op_gpu_GPU
 * Method:    releaseAll
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_uk_ac_imperial_lsds_streamsql_op_gpu_GPU_releaseAll
  (JNIEnv *, jobject);

/*
 * Class:     uk_ac_imperial_lsds_streamsql_op_gpu_GPU
 * Method:    invokeSelectionOperatorKernel
 * Signature: ([Ljava/lang/Object;)I
 */
JNIEXPORT jint JNICALL Java_uk_ac_imperial_lsds_streamsql_op_gpu_GPU_invokeSelectionOperatorKernel
  (JNIEnv *, jobject, jobjectArray);

/*
 * Class:     uk_ac_imperial_lsds_streamsql_op_gpu_GPU
 * Method:    invokeProjectionOperatorKernel
 * Signature: ([Ljava/lang/Object;)I
 */
JNIEXPORT jint JNICALL Java_uk_ac_imperial_lsds_streamsql_op_gpu_GPU_invokeProjectionOperatorKernel
  (JNIEnv *, jobject, jobjectArray);

/*
 * Class:     uk_ac_imperial_lsds_streamsql_op_gpu_GPU
 * Method:    invokeMicroAggregationOperatorKernel
 * Signature: ([Ljava/lang/Object;)I
 */
JNIEXPORT jint JNICALL Java_uk_ac_imperial_lsds_streamsql_op_gpu_GPU_invokeMicroAggregationOperatorKernel
  (JNIEnv *, jobject, jobjectArray);

/*
 * Class:     uk_ac_imperial_lsds_streamsql_op_gpu_GPU
 * Method:    invokeReductionOperatorKernel
 * Signature: (IIZ)I
 */
JNIEXPORT jint JNICALL Java_uk_ac_imperial_lsds_streamsql_op_gpu_GPU_invokeReductionOperatorKernel
  (JNIEnv *, jobject, jint, jint, jboolean);

/*
 * Class:     uk_ac_imperial_lsds_streamsql_op_gpu_GPU
 * Method:    invokeInputDataMovementCallback
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_uk_ac_imperial_lsds_streamsql_op_gpu_GPU_invokeInputDataMovementCallback
  (JNIEnv *, jobject);

/*
 * Class:     uk_ac_imperial_lsds_streamsql_op_gpu_GPU
 * Method:    invokeGPUWrite
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_uk_ac_imperial_lsds_streamsql_op_gpu_GPU_invokeGPUWrite
  (JNIEnv *, jobject);

/*
 * Class:     uk_ac_imperial_lsds_streamsql_op_gpu_GPU
 * Method:    invokeGPURead
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_uk_ac_imperial_lsds_streamsql_op_gpu_GPU_invokeGPURead
  (JNIEnv *, jobject);

/*
 * Class:     uk_ac_imperial_lsds_streamsql_op_gpu_GPU
 * Method:    invokeOutputDataMovementCallback
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_uk_ac_imperial_lsds_streamsql_op_gpu_GPU_invokeOutputDataMovementCallback
  (JNIEnv *, jobject);

/*
 * Class:     uk_ac_imperial_lsds_streamsql_op_gpu_GPU
 * Method:    invokeAlternativeInputDataMovementCallback
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_uk_ac_imperial_lsds_streamsql_op_gpu_GPU_invokeAlternativeInputDataMovementCallback
  (JNIEnv *, jobject);

/*
 * Class:     uk_ac_imperial_lsds_streamsql_op_gpu_GPU
 * Method:    invokeAlternativeGPUWrite
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_uk_ac_imperial_lsds_streamsql_op_gpu_GPU_invokeAlternativeGPUWrite
  (JNIEnv *, jobject);

/*
 * Class:     uk_ac_imperial_lsds_streamsql_op_gpu_GPU
 * Method:    invokeAlternativeGPURead
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_uk_ac_imperial_lsds_streamsql_op_gpu_GPU_invokeAlternativeGPURead
  (JNIEnv *, jobject);

/*
 * Class:     uk_ac_imperial_lsds_streamsql_op_gpu_GPU
 * Method:    invokeAlternativeOutputDataMovementCallback
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_uk_ac_imperial_lsds_streamsql_op_gpu_GPU_invokeAlternativeOutputDataMovementCallback
  (JNIEnv *, jobject);

/*
 * Class:     uk_ac_imperial_lsds_streamsql_op_gpu_GPU
 * Method:    invokeNullKernel
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_uk_ac_imperial_lsds_streamsql_op_gpu_GPU_invokeNullKernel
  (JNIEnv *, jobject);

#ifdef __cplusplus
}
#endif
#endif