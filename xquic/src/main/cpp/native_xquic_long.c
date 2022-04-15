//
// Created by yqy on 2022/3/23.
//
#include <xquic_common.h>
#include "native_xquic_long.h"
#include "xquic_client_long.h"
#include "native_xquic_common.h"

/**
* 链接
*/
JNIEXPORT jlong JNICALL Java_com_lizhi_component_net_xquic_native_XquicLongNative_connect
        (JNIEnv *env, jclass cls, jobject param, jobject callback) {
    xqc_cli_user_data_params_t *user_param = get_data_params(env, param, callback);
    if (user_param == NULL) {
        return -1;
    }

    xqc_cli_ctx_t *ctx = client_long_conn(user_param);
    if (ctx == NULL) {
        return -1;
    }
    return ptr_to_jlong(ctx);
}


/**
* 开始
 */
JNIEXPORT jint JNICALL Java_com_lizhi_component_net_xquic_native_XquicLongNative_start
        (JNIEnv *env, jclass cls, jlong clientCtx) {
    return client_long_start(jlong_to_ptr(clientCtx));
}

JNIEXPORT jint JNICALL Java_com_lizhi_component_net_xquic_native_XquicLongNative_sendPing
        (JNIEnv *env, jclass cls, jlong clientCtx, jstring pingContent) {
    return client_long_send_ping(jlong_to_ptr(clientCtx), pingContent);
}
/**
* 发送数据
 */
JNIEXPORT jint JNICALL Java_com_lizhi_component_net_xquic_native_XquicLongNative_send
        (JNIEnv *env, jclass cls, jlong clientCtx, jstring content) {
    return client_long_send(jlong_to_ptr(clientCtx), content);
}

/**
* 取消发送数据
*/
JNIEXPORT jint JNICALL Java_com_lizhi_component_net_xquic_native_XquicLongNative_cancel
        (JNIEnv *env, jclass cls, jlong clientCtx) {
    return client_long_cancel(jlong_to_ptr(clientCtx));
}