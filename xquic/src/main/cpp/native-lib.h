//
// Created by 杨青远 on 2/17/22.
//

#include<jni.h>

#ifndef _Hncluded_com_lizhi_component_net_xquic_native_XquicNative
#define _Hncluded_com_lizhi_component_net_xquic_native_XquicNative

#ifdef __cplusplus

extern "C"{
#endif

JNIEXPORT void JNICALL Java_com_lizhi_component_net_xquic_native_XquicNative_init
        (JNIEnv* env,jobject obj ,jstring host ,int port);

JNIEXPORT void JNICALL Java_com_lizhi_component_net_xquic_native_XquicNative_send
        (JNIEnv *env, jclass cls,jstring content);

#ifdef __cplusplus
}
#endif
#endif
