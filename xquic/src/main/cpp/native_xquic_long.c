//
// Created by lizhi on 2022/3/23.
//
#include "native_xquic_long.h"

/**
* 链接
*/
JNIEXPORT jlong JNICALL Java_com_lizhi_component_net_xquic_native_XquicLongNative_connect
        (JNIEnv *env, jclass cls,jstring host ,jint port,jstring token,jstring session){

    return 0;
}

/**
* 发送数据
 */
JNIEXPORT jint JNICALL Java_com_lizhi_component_net_xquic_native_XquicLongNative_send
        (JNIEnv *env, jclass cls,jlong clientCtx, jstring content){

    return 0;
}

/**
* 取消发送数据
*/
JNIEXPORT jint JNICALL Java_com_lizhi_component_net_xquic_native_XquicLongNative_cancel
        (JNIEnv *env, jclass cls,jlong clientCtx){

    return 0;
}