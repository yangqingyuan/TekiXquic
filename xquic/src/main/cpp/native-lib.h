//
// Created by 杨青远 on 2/17/22.
//

#include<jni.h>

#ifndef _Hncluded_com_lizhi_component_net_xquic_native_XquicNative
#define _Hncluded_com_lizhi_component_net_xquic_native_XquicNative

#ifdef __cplusplus

extern "C"{
#endif

/**
* xquic 初始化
*/
JNIEXPORT jlong JNICALL Java_com_lizhi_component_net_xquic_native_XquicNative_xquicInit();

/**
* 链接
*/
JNIEXPORT jint JNICALL Java_com_lizhi_component_net_xquic_native_XquicNative_xquicConnect
        (JNIEnv *env, jclass cls,jlong clientCtx,jstring host ,jint port,jstring token,jstring session);

/**
* 发送数据
*/
JNIEXPORT jint JNICALL Java_com_lizhi_component_net_xquic_native_XquicNative_xquicSend
        (JNIEnv *env, jclass cls,jlong clientCtx,jstring content);

/**
* H3的方式发送数据
*/
JNIEXPORT jint JNICALL Java_com_lizhi_component_net_xquic_native_XquicNative_xquicH3Send
        (JNIEnv *env, jclass cls,jlong clientCtx,jstring content);

/**
* 销毁
*/
JNIEXPORT jint JNICALL Java_com_lizhi_component_net_xquic_native_XquicNative_xquicDestroy
        (JNIEnv *env, jclass cls,jlong clientCtx);

#ifdef __cplusplus
}
#endif
#endif
