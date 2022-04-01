//
// Created by lizhi on 2022/3/23.
//
#include <xquic_common.h>
#include "native_xquic_short.h"
#include "xquic_client_short.h"

/**
 * meg type
 */
typedef enum client_msg_type {
    MSG_TYPE_TOKEN,//token
    MSG_TYPE_SESSION,//session
    MSG_TYPE_TP,//tp
    MSG_TYPE_DATA//service rev data
} MSG_TYPE;


void
callback_to_java(void *ev_android, void *object_android, int msg_type, const unsigned char *data,
                 unsigned len) {
    if (len <= 0) {
        LOGW("call back java error,can len = %d", len);
        return;
    }
    JNIEnv *env = (JNIEnv *) ev_android;

    /* find class and get method */
    jclass callbackClass = (*env)->GetObjectClass(env, object_android);
    jobject j_obj = (*env)->NewGlobalRef(env, object_android);//关键，要不会崩溃
    jmethodID jmid = (*env)->GetMethodID(env, callbackClass, "callBackMessage","(I[B)V");
    if (!jmid) {
        LOGE("call back java error,can not find methodId callBackMessage");
        return;
    }

    /* data to byteArray*/
    jbyteArray dataBuf = (*env)->NewByteArray(env, len);
    (*env)->SetByteArrayRegion(env, dataBuf, 0, len, (jbyte *) data);

    /* call back */
    (*env)->CallVoidMethod(env, j_obj, jmid, msg_type, dataBuf);

    /* free */
    (*env)->DeleteGlobalRef(env, j_obj);
    (*env)->DeleteLocalRef(env, dataBuf);
}

void callback_token(void *ev_android, void *object_android, const unsigned char *data,
                    unsigned len) {
    DEBUG;
    callback_to_java(ev_android, object_android, MSG_TYPE_TOKEN, data, len);
}

void callback_session(void *ev_android, void *object_android, const char *data, size_t len) {
    DEBUG;
    callback_to_java(ev_android, object_android, MSG_TYPE_SESSION, data, len);
}


void callback_tp(void *ev_android, void *object_android, const char *data, size_t len) {
    DEBUG;
    callback_to_java(ev_android, object_android, MSG_TYPE_TP, data, len);
}


/**
 *
 * @return callback data to java
 */
int callback_read_data(void *ev_android, void *object_android, int ret, char *data, ssize_t len) {
    JNIEnv *env = (JNIEnv *) ev_android;

    /* find class and get method */
    jclass callbackClass = (*env)->GetObjectClass(env, object_android);
    jobject j_obj = (*env)->NewGlobalRef(env, object_android);//关键，要不会崩溃
    jmethodID jmid = (*env)->GetMethodID(env, callbackClass, "callBackReadData", "(I[B)V");
    if (!jmid) {
        LOGE("call back error,can not find methodId callBackReadData");
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

jstring getString(JNIEnv *env, jobject param, const char *field) {
    jclass sendParamsClass = (*env)->GetObjectClass(env, param);
    jfieldID jfieldId = (*env)->GetFieldID(env, sendParamsClass, field, "Ljava/lang/String;");
    if (!jfieldId) {
        return NULL;
    }
    jstring string = (jstring) (*env)->GetObjectField(env, param, jfieldId);
    return string;
}

jint getInt(JNIEnv *env, jobject param, const char *field) {
    jclass sendParamsClass = (*env)->GetObjectClass(env, param);
    jfieldID jfieldId = (*env)->GetFieldID(env, sendParamsClass, field, "I");
    if (!jfieldId) {
        return 0;
    }
    jint data = (*env)->GetIntField(env, param, jfieldId);
    return data;
}


/*
 * get params
 */
xqc_cli_user_data_params_t *get_data_params(JNIEnv *env, jobject param, jobject callback) {

    jstring url = getString(env, param, "url");
    jstring content = getString(env, param, "content");

    if (url == NULL || content == NULL) {
        LOGE("xquicConnect error url == NULL or content can not null");
        return NULL;
    }

    jstring token = getString(env, param, "token");
    jstring session = getString(env, param, "session");
    jint time_out = getInt(env, param, "timeOut");
    jint max_recv_data_len = getInt(env, param, "maxRecvDataLen");
    jint cc_type = getInt(env, param, "ccType");


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
    xqc_cli_user_data_params_t *user_cfg = malloc(sizeof(xqc_cli_user_data_params_t));

    /* key param */
    user_cfg->url = cUrl;
    user_cfg->content = cContent;

    /* optional param */
    user_cfg->token = cToken;
    user_cfg->session = cSession;
    user_cfg->conn_timeout = time_out;
    user_cfg->max_recv_data_len = max_recv_data_len;

    switch (cc_type) {
        case 0:
            user_cfg->cc = CC_TYPE_BBR;
            break;
        case 1:
            user_cfg->cc = CC_TYPE_CUBIC;
            break;
        case 2:
            user_cfg->cc = CC_TYPE_RENO;
            break;
        default:
            user_cfg->cc = CC_TYPE_BBR;
    }

    /* callback */
    user_cfg->user_data_callback.env_android = env;
    user_cfg->user_data_callback.object_android = callback;
    user_cfg->user_data_callback.callback_read_data = callback_read_data;
    user_cfg->user_data_callback.callback_token = callback_token;
    user_cfg->user_data_callback.callback_session = callback_session;
    user_cfg->user_data_callback.callback_pt = callback_tp;

    return user_cfg;
}


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
    client_send(user_param);
    return 0;
}