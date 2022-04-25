//
// Created by lizhi on 2022/3/23.
//
#include <xquic_common.h>
#include "native_xquic_common.h"
#include "native_xquic_short.h"
#include "xquic_client_short.h"

/**
* 发送数据
 */
JNIEXPORT jint JNICALL Java_com_lizhi_component_net_xquic_native_XquicShortNative_send
        (JNIEnv *env, jclass cls, jobject param, jobject callback) {
    xqc_cli_user_data_params_t *user_param = get_data_params(env, param, callback);
    if (user_param == NULL) {
        return -1;
    }
    /* start to send data */
    client_short_send(user_param);
    return 0;
}


JNIEXPORT jint JNICALL Java_com_lizhi_component_net_xquic_native_XquicShortNative_cancel
        (JNIEnv *env, jclass cls, jlong clientCtx) {
    return client_short_cancel(jlong_to_ptr(clientCtx));
}