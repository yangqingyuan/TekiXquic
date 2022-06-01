#include <xquic_common.h>
#include <assert.h>
#include "native_xquic_common.h"

static JavaVM *g_jvm;
static pthread_mutex_t mutex;

/**
 * callback msg to java
 * @param object_android
 * @param msg_type
 * @param data
 * @param len
 * @param user_data
 */
void callback_msg_to_java(void *object_android, MSG_TYPE msg_type, const char *data,
                          unsigned len, void *user_data) {
    JNIEnv *env;
    (*g_jvm)->AttachCurrentThread(g_jvm, &env, NULL);

    /* find class and get method */
    jclass callbackClass = (*env)->GetObjectClass(env, object_android);
    jobject j_obj = (*env)->NewGlobalRef(env, object_android);//关键，要不会崩溃
    jmethodID jmid = (*env)->GetMethodID(env, callbackClass, "callBackMessage",
                                         "(ILjava/lang/String;)V");
    if (!jmid) {
        LOGE("call back java error,can not find methodId callBackMessage");
        return;
    }

    /* data to jstring*/
    jstring recv_body;
    if (data != NULL) {
        recv_body = (*env)->NewStringUTF(env, data);
    } else {
        recv_body = (*env)->NewStringUTF(env, "");
    }

    /* call back */
    (*env)->CallVoidMethod(env, j_obj, jmid, msg_type, recv_body);

    /* free */
    (*env)->DeleteGlobalRef(env, j_obj);
    (*env)->DeleteLocalRef(env, recv_body);
    (*env)->DeleteLocalRef(env, callbackClass);

    if (msg_type == MSG_TYPE_DESTROY) {//destroy global ref
        (*env)->DeleteGlobalRef(env, object_android);
    }
}


/**
 *
 * @param object_android
 * @param core
 * @param data
 * @param len
 * @param user_data
 * @return callback data to java
 */
int callback_data_to_java(void *object_android, int core, const char *data, ssize_t len,
                          void *user_data) {

    JNIEnv *env;
    (*g_jvm)->AttachCurrentThread(g_jvm, &env, NULL);

    /* find class and get method */
    jclass callbackClass = (*env)->GetObjectClass(env, object_android);
    jobject j_obj = (*env)->NewGlobalRef(env, object_android);//关键，要不会崩溃
    jmethodID jm_id = (*env)->GetMethodID(env, callbackClass, "callBackData",
                                          "(ILjava/lang/String;)V");
    if (!jm_id) {
        LOGE("call back error,can not find methodId callBackReadData");
        return -1;
    }

    cJSON *usr = cJSON_CreateObject();
    cJSON_AddStringToObject(usr, "recv_body", data);
    if (user_data != NULL) {
        cJSON_AddStringToObject(usr, "tag", user_data);
    }
    char *json_data = cJSON_Print(usr);

    /* data to jstring*/
    jstring recv_body = (*env)->NewStringUTF(env, json_data);

    /* call back */
    (*env)->CallVoidMethod(env, j_obj, jm_id, core, recv_body);

    /* free */
    (*env)->DeleteGlobalRef(env, j_obj);
    (*env)->DeleteLocalRef(env, recv_body);
    (*env)->DeleteLocalRef(env, callbackClass);
    cJSON_free(json_data);
    cJSON_Delete(usr);
    return 0;
}

jstring getString(JNIEnv *env, jobject param, const char *field) {
    jclass sendParamsClass = (*env)->GetObjectClass(env, param);
    jfieldID jfieldId = (*env)->GetFieldID(env, sendParamsClass, field, "Ljava/lang/String;");
    if (!jfieldId) {
        return NULL;
    }
    jstring string = (jstring) (*env)->GetObjectField(env, param, jfieldId);
    (*env)->DeleteLocalRef(env, sendParamsClass);
    return string;
}

jint getInt(JNIEnv *env, jobject param, const char *field) {
    jclass sendParamsClass = (*env)->GetObjectClass(env, param);
    jfieldID jfieldId = (*env)->GetFieldID(env, sendParamsClass, field, "I");
    if (!jfieldId) {
        return 0;
    }
    jint data = (*env)->GetIntField(env, param, jfieldId);
    (*env)->DeleteLocalRef(env, sendParamsClass);
    return data;
}


int build_headers_from_params(JNIEnv *env, jobject param, const char *field,
                              xqc_http_header_t *heards) {
    jclass sendParamsClass = (*env)->GetObjectClass(env, param);
    jfieldID jfieldId = (*env)->GetFieldID(env, sendParamsClass, field, "Ljava/util/HashMap;");
    if (!jfieldId) {
        return -1;
    }
    jobject headersHashMapObject = (*env)->GetObjectField(env, param, jfieldId);
    if (!headersHashMapObject) {
        return -1;
    }

    jclass hashMapClass = (*env)->FindClass(env, "java/util/HashMap");
    jmethodID entrySetMID = (*env)->GetMethodID(env, hashMapClass, "entrySet", "()Ljava/util/Set;");

    jobject setObj = (*env)->CallObjectMethod(env, headersHashMapObject, entrySetMID);
    jclass setClass = (*env)->FindClass(env, "java/util/Set");
    jmethodID iteratorMID = (*env)->GetMethodID(env, setClass, "iterator",
                                                "()Ljava/util/Iterator;");

    jobject iteratorObj = (*env)->CallObjectMethod(env, setObj, iteratorMID);
    jclass iteratorClass = (*env)->FindClass(env, "java/util/Iterator");
    jmethodID hashNextMID = (*env)->GetMethodID(env, iteratorClass, "hasNext", "()Z");
    jmethodID nextMID = (*env)->GetMethodID(env, iteratorClass, "next", "()Ljava/lang/Object;");

    jclass entryClass = (*env)->FindClass(env, "java/util/Map$Entry");
    jmethodID getKeyMID = (*env)->GetMethodID(env, entryClass, "getKey", "()Ljava/lang/Object;");
    jmethodID getValueMID = (*env)->GetMethodID(env, entryClass, "getValue",
                                                "()Ljava/lang/Object;");

    int i = 0;
    while ((*env)->CallBooleanMethod(env, iteratorObj, hashNextMID)) {
        jobject entryObj = (*env)->CallObjectMethod(env, iteratorObj, nextMID);

        jstring keyString = (*env)->CallObjectMethod(env, entryObj, getKeyMID);
        const char *keyChar = (*env)->GetStringUTFChars(env, keyString, 0);

        jstring valueString = (*env)->CallObjectMethod(env, entryObj, getValueMID);
        const char *valueChar = (*env)->GetStringUTFChars(env, valueString, 0);

        xqc_http_header_t header = {
                .name = {.iov_base = keyChar, .iov_len = strlen(keyChar)},
                .value = {.iov_base = valueChar, .iov_len = strlen(valueChar)},
                .flags = 0,
        };
        heards[i] = header;
        i++;
    }

    (*env)->DeleteLocalRef(env, sendParamsClass);
    (*env)->DeleteLocalRef(env, headersHashMapObject);
    (*env)->DeleteLocalRef(env, hashMapClass);
    (*env)->DeleteLocalRef(env, setObj);
    (*env)->DeleteLocalRef(env, setClass);
    (*env)->DeleteLocalRef(env, iteratorObj);
    (*env)->DeleteLocalRef(env, iteratorClass);
    (*env)->DeleteLocalRef(env, entryClass);

    return 0;
}


/*
 * get params
 */
xqc_cli_user_data_params_t *get_data_params(JNIEnv *env, jobject param, jobject callback) {

    jstring url = getString(env, param, "url");

    if (url == NULL) {
        LOGE("xquicConnect error url == NULL");
        return NULL;
    }

    jobject gl_callback = (*env)->NewGlobalRef(env, callback);

    jstring token = getString(env, param, "token");
    jstring session = getString(env, param, "session");
    jint connect_time_out = getInt(env, param, "connectTimeOut");
    jint read_time_out = getInt(env, param, "readTimeOut");
    jint max_recv_data_len = getInt(env, param, "maxRecvDataLen");
    jint cc_type = getInt(env, param, "ccType");
    jint headersSize = getInt(env, param, "headersSize");

    /* build header from params */
    xqc_http_header_t *headers = malloc(sizeof(xqc_http_header_t) * headersSize);
    if (build_headers_from_params(env, param, "headers", headers) < 0) {
        LOGE("build_headers_from_params error");
        return NULL;
    }

    const char *cUrl = (*env)->GetStringUTFChars(env, url, 0);

    jstring content = getString(env, param, "content");
    const char *cContent = NULL;
    if (content != NULL) {
        cContent = (*env)->GetStringUTFChars(env, content, 0);
    }

    const char *cToken = NULL;
    if (token != NULL) {
        cToken = (*env)->GetStringUTFChars(env, token, 0);
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
    user_cfg->conn_timeout = connect_time_out;
    user_cfg->read_timeout = read_time_out;
    user_cfg->max_recv_data_len = max_recv_data_len;
    user_cfg->mutex = &mutex;

    /* headers */
    user_cfg->h3_hdrs.headers = headers;
    user_cfg->h3_hdrs.count = headersSize;

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
    user_cfg->user_data_callback.object_android = gl_callback;
    user_cfg->user_data_callback.callback_data = callback_data_to_java;
    user_cfg->user_data_callback.callback_msg = callback_msg_to_java;
    return user_cfg;
}


JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    DEBUG;
    JNIEnv *env = NULL;
    g_jvm = vm;
    if ((*vm)->GetEnv(vm, (void **) &env, JNI_VERSION_1_4) != JNI_OK) {
        return -1;
    }
    assert(env != NULL);
    /* init lock */
    pthread_mutex_init(&mutex, NULL);
    return JNI_VERSION_1_4;
}

JNIEXPORT void JNI_OnUnload(JavaVM *jvm, void *reserved) {
    DEBUG;
    pthread_mutex_destroy(&mutex);
}
