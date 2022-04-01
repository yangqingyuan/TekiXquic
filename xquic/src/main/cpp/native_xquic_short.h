//
// Created by lizhi on 2022/3/23.
//

#include<jni.h>
#include "common.h"

#ifndef _Hncluded_com_lizhi_component_net_xquic_native_XquicShortNative
#define _Hncluded_com_lizhi_component_net_xquic_native_XquicShortNative

#ifdef __cplusplus

extern "C"{
#endif

/**
* 发送数据
 */
JNIEXPORT jint JNICALL Java_com_lizhi_component_net_xquic_native_XquicShortNative_send
        (JNIEnv *env, jclass cls,jobject params,
         jobject callback);


#ifdef __cplusplus
}
#endif
#endif