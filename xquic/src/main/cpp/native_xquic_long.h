//
// Created by lizhi on 2022/3/23.
//

#include<jni.h>
#include "common.h"

#ifndef _Hncluded_com_lizhi_component_net_xquic_native_XquicLongNative
#define _Hncluded_com_lizhi_component_net_xquic_native_XquicLongNative

#ifdef __cplusplus

extern "C"{
#endif

/**
* 链接
*/
JNIEXPORT jlong JNICALL Java_com_lizhi_component_net_xquic_native_XquicLongNative_connect
        (JNIEnv *env, jclass cls, jobject params, jobject callback);


/**
* 开始
 */
JNIEXPORT jint JNICALL Java_com_lizhi_component_net_xquic_native_XquicLongNative_start
        (JNIEnv *env, jclass cls, jlong clientCtx);


/**
* 发送ping数据
 */
JNIEXPORT jint JNICALL Java_com_lizhi_component_net_xquic_native_XquicLongNative_sendPing
        (JNIEnv *env, jclass cls, jlong clientCtx, jstring pingContent);

/**
* 发送数据
 */
JNIEXPORT jint JNICALL Java_com_lizhi_component_net_xquic_native_XquicLongNative_send
        (JNIEnv *env, jclass cls, jlong clientCtx, jstring content);


/**
* 发送带头的数据，域名相同，path不相同的情况，用于链接复用
 */
JNIEXPORT jint JNICALL Java_com_lizhi_component_net_xquic_native_XquicLongNative_sendWithHead
        (JNIEnv *env, jclass cls, jlong clientCtx, jobject params, jstring content);

/**
* 取消发送数据
*/
JNIEXPORT jint JNICALL Java_com_lizhi_component_net_xquic_native_XquicLongNative_cancel
        (JNIEnv *env, jclass cls, jlong clientCtx);

#ifdef __cplusplus
}
#endif
#endif