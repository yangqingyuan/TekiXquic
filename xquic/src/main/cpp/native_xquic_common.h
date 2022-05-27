//
// Created by lizhi on 2022/3/23.
//

#include<jni.h>
#include "common.h"

#ifndef _Hncluded_com_lizhi_component_net_xquic_native_XquicCommonNative
#define _Hncluded_com_lizhi_component_net_xquic_native_XquicCommonNative

#ifdef __cplusplus

extern "C"{
#endif


xqc_cli_user_data_params_t *get_data_params(JNIEnv *env, jobject param, jobject callback);


int build_headers_from_params(JNIEnv *env, jobject param, const char *field,
                              xqc_http_header_t *heards);

jint getInt(JNIEnv *env, jobject param, const char *field);


#ifdef __cplusplus
}
#endif
#endif