//
// Created by lizhi on 2022/3/23.
//
#include <xquic_common.h>
#include "native_xquic_short.h"
#include "xquic_client_short.h"

/**
 * callback data to java
 * @param ev_android
 * @param object_android
 * @param ret
 * @param data
 * @param len
 * @return
 */
int read_data_callback(void *ev_android, void *object_android, int ret, char *data, ssize_t len) {
    JNIEnv *env = (JNIEnv *) ev_android;

    /* find class and get method */
    jclass callbackClass = (*env)->GetObjectClass(env, object_android);
    jobject j_obj = (*env)->NewGlobalRef(env, object_android);//关键，要不会崩溃
    jmethodID jmid = (*env)->GetMethodID(env, callbackClass, "callBack", "(I[B)V");
    if (!jmid) {
        LOGE("call back error,can not find methodId callBack");
        return -1;
    }

    /* data to byteArray*/
    jbyteArray dataBuf = (*env)->NewByteArray(env, len);
    (*env)->SetByteArrayRegion(env, dataBuf, 0, len, (jbyte *) data);

    /* call back */
    (*env)->CallVoidMethod(env, j_obj, jmid, ret, dataBuf);

    /* free */
    (*env)->DeleteGlobalRef(env, j_obj);
    (*env)->DeleteLocalRef(env, dataBuf);
    return 0;
}


/**
* 发送数据
 */
JNIEXPORT jint JNICALL Java_com_lizhi_component_net_xquic_native_XquicShortNative_send
        (JNIEnv *env, jclass cls, jstring url, jstring token, jstring session,
         jstring content, jobject callback) {
    if (url == NULL || content == NULL) {
        LOGE("xquicConnect error url == NULL or content can not null");
        return -1;
    }

    const char *cUrl = (*env)->GetStringUTFChars(env, url, 0);
    const char *cContent = (*env)->GetStringUTFChars(env, content, 0);

    const char *cToken = NULL;
    if (token != NULL) {
        (*env)->GetStringUTFChars(env, token, 0);
    }

    const char *cSession = NULL;
    if (session != NULL) {
        cSession = (*env)->GetStringUTFChars(env, session, 0);
    }

    /* user custom callback */
    xqc_cli_user_callback_t user_cfg = {
            .env_android = env,//use to read_data_callback
            .object_android = callback, //use to read_data_callback
            .read_data_callback =read_data_callback
    };

    /* start to send data */
    client_send(cUrl, cToken, cSession, cContent, &user_cfg);

    return 0;
}