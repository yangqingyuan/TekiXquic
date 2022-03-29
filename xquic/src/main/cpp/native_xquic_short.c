//
// Created by lizhi on 2022/3/23.
//
#include "native_xquic_short.h"
#include "xquic_client_short.h"

/**
* 发送数据
 */
JNIEXPORT jint JNICALL Java_com_lizhi_component_net_xquic_native_XquicShortNative_send
        (JNIEnv *env, jclass cls, jstring host, jint port, jstring token, jstring session,
         jstring content) {
    if (host == NULL) {
        LOGE("xquicConnect error host == NULL");
        return -1;
    }

    if (content == NULL) {
        LOGE("content can not null");
        return -1;
    }
    const char *cHost = (*env)->GetStringUTFChars(env, host, 0);

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

    /* 开始发送数据 */
    client_send(cHost, port, cToken, cSession, cContent);

    (*env)->ReleaseStringUTFChars(env, host, cHost);
    (*env)->DeleteLocalRef(env, host);

    (*env)->ReleaseStringUTFChars(env, host, cContent);
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