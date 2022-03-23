//
// Created by lizhi on 2022/3/23.
//
#include "native_xquic_short.h"

/**
* 发送数据
 */
JNIEXPORT jlong JNICALL Java_com_lizhi_component_net_xquic_native_XquicShortNative_send
        (JNIEnv *env, jclass cls,jstring host ,jint port,jstring token,jstring session,jstring content){

    return 0;
}


/**
* 取消发送数据
*/
JNIEXPORT jint JNICALL Java_com_lizhi_component_net_xquic_native_XquicShortNative_cancle
        (JNIEnv *env, jclass cls,jlong clientCtx){

    return 0;
}
