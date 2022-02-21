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
JNIEXPORT long JNICALL Java_com_lizhi_component_net_xquic_native_XquicNative_xquicInit
        (JNIEnv* env,jobject obj ,jstring host ,int port,jstring token,jstring session);

/**
* 开始
*/
JNIEXPORT int JNICALL Java_com_lizhi_component_net_xquic_native_XquicNative_xquicStart
        (JNIEnv *env, jclass cls,long clientCtx,jstring content);

/**
* 发送数据
*/
JNIEXPORT int JNICALL Java_com_lizhi_component_net_xquic_native_XquicNative_xquicSend
        (JNIEnv *env, jclass cls,long clientCtx,jstring content);

/**
* 销毁
*/
JNIEXPORT int JNICALL Java_com_lizhi_component_net_xquic_native_XquicNative_xquicDestroy
        (JNIEnv *env, jclass cls,long clientCtx,jstring content);

#ifdef __cplusplus
}
#endif
#endif
