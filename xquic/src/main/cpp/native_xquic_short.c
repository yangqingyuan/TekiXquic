//
// Created by lizhi on 2022/3/23.
//
#include <xquic_common.h>
#include "native_xquic_short.h"
#include "xquic_client_short.h"


int read_data_callback(int core, char *data, ssize_t len) {
    LOGE("回调：core=%d,data=%s,len=%ld", core, data, len);
    return 0;
}


/**
* 发送数据
 */
JNIEXPORT jint JNICALL Java_com_lizhi_component_net_xquic_native_XquicShortNative_send
        (JNIEnv *env, jclass cls, jstring url, jstring token, jstring session,
         jstring content) {
    if (url == NULL) {
        LOGE("xquicConnect error host == NULL");
        return -1;
    }

    if (content == NULL) {
        LOGE("content can not null");
        return -1;
    }
    const char *cUrl = (*env)->GetStringUTFChars(env, url, 0);

    const char *cToken = NULL;
    if (token != NULL) {
        cToken = (*env)->GetStringUTFChars(env, token, 0);
    }

    const char *cSession = NULL;
    if (session != NULL) {
        cSession = (*env)->GetStringUTFChars(env, session, 0);
    }

    const char *cContent = NULL;
    if (content != NULL) {
        cContent = (*env)->GetStringUTFChars(env, content, 0);
    }

    /* 设置请求回调 */
    xqc_cli_user_callback_t user_cfg = {
            .read_data_callback =read_data_callback
    };

    /* 开始发送数据 */
    client_send(cUrl, cToken, cSession, cContent, &user_cfg);

    (*env)->ReleaseStringUTFChars(env, url, cUrl);
    (*env)->DeleteLocalRef(env, url);

    (*env)->ReleaseStringUTFChars(env, content, cContent);
    (*env)->DeleteLocalRef(env, content);

    if (session != NULL) {
        (*env)->ReleaseStringUTFChars(env, session, cSession);
        (*env)->DeleteLocalRef(env, session);
    }

    if (token != NULL) {
        (*env)->ReleaseStringUTFChars(env, token, cToken);
        (*env)->DeleteLocalRef(env, token);
    }
    return 0;
}