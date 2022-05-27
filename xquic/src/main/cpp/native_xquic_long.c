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

/**
 * 发送ping数据
 * @param env
 * @param cls
 * @param clientCtx
 * @param pingContent
 * @return
 */
JNIEXPORT jint JNICALL Java_com_lizhi_component_net_xquic_native_XquicLongNative_sendPing
        (JNIEnv *env, jclass cls, jlong clientCtx, jstring pingContent) {
    return client_long_send_ping(jlong_to_ptr(clientCtx),
                                 (*env)->GetStringUTFChars(env, pingContent, 0));
}

/**
* 发送数据
 */
JNIEXPORT jint JNICALL Java_com_lizhi_component_net_xquic_native_XquicLongNative_send
        (JNIEnv *env, jclass cls, jlong clientCtx, jstring content) {
    return client_long_send(jlong_to_ptr(clientCtx), (*env)->GetStringUTFChars(env, content, 0));
}

/**
* 发送带头的数据，域名相同，path不相同的情况，用于链接复用
 */
JNIEXPORT jint JNICALL Java_com_lizhi_component_net_xquic_native_XquicLongNative_sendWithHead
        (JNIEnv *env, jclass cls, jlong clientCtx, jobject param, jstring content) {
    jint headersSize = getInt(env, param, "headersSize");
    /* build header from params */
    xqc_http_header_t *headers = malloc(sizeof(xqc_http_header_t) * headersSize);
    if (build_headers_from_params(env, param, "headers", headers) < 0) {
        LOGE("build_headers_from_params error");
        return -1;
    }
    return client_long_send_with_head(jlong_to_ptr(clientCtx), headers, headersSize,
                                      (*env)->GetStringUTFChars(env, content, 0));
}

/**
* 取消发送数据
*/
JNIEXPORT jint JNICALL Java_com_lizhi_component_net_xquic_native_XquicLongNative_cancel
        (JNIEnv *env, jclass cls, jlong clientCtx) {
    return client_long_cancel(jlong_to_ptr(clientCtx));
}