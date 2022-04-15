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


/**
 * meg type
 */
typedef enum client_msg_type {
    MSG_TYPE_TOKEN,//token
    MSG_TYPE_SESSION,//session
    MSG_TYPE_TP,//tp
    MSG_TYPE_HEAD,//head
    MSG_TYPE_PING//ping data
} MSG_TYPE;


xqc_cli_user_data_params_t *get_data_params(JNIEnv *env, jobject param, jobject callback);

#ifdef __cplusplus
}
#endif
#endif