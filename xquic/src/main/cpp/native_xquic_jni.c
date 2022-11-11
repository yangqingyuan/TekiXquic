#include <xquic_common.h>
#include <assert.h>
#include "xquic_client_short.h"
#include "xquic_client_long.h"

#define JNI_CLASS_XQUIC_SHORT   "com/lizhi/component/net/xquic/quic/XquicShortNative"
#define JNI_CLASS_XQUIC_LONG   "com/lizhi/component/net/xquic/quic/XquicLongNative"

typedef struct xquic_fields_t {
    jclass clazz_short;
    jclass clazz_long;
    JavaVM *g_jvm;
    pthread_mutex_t mutex;
} xquic_fields_t;
static xquic_fields_t g_clazz;

#ifndef NELEM
#define NELEM(x) ((int) (sizeof(x) / sizeof((x)[0])))
#endif

int J4A_ExceptionCheck__catchAll(JNIEnv *env) {
    if ((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
        return 1;
    }
    return 0;
}

#define XQUIC_FIND_JAVA_CLASS(env__, var__, classsign__) \
    do { \
        jclass clazz = (*env__)->FindClass(env__, classsign__); \
        if (J4A_ExceptionCheck__catchAll(env) || !(clazz)) { \
            LOGE("FindClass failed: %s", classsign__); \
            return -1; \
        } \
        var__ = (*env__)->NewGlobalRef(env__, clazz); \
        if (J4A_ExceptionCheck__catchAll(env) || !(var__)) { \
            LOGE("FindClass::NewGlobalRef failed: %s", classsign__); \
            (*env__)->DeleteLocalRef(env__, clazz); \
            return -1; \
        } \
        (*env__)->DeleteLocalRef(env__, clazz); \
    } while(0);


/**
 * callback msg to java
 * @param object_android
 * @param msg_type
 * @param data
 * @param len
 * @param user_data
 */
static void callback_msg_to_java(void *object_android, MSG_TYPE msg_type, const char *data,
                                 uint32_t len, void *user_data) {
    JNIEnv *env;
    (*g_clazz.g_jvm)->AttachCurrentThread(g_clazz.g_jvm, &env, NULL);

    /* find class and get method */
    jclass callbackClass = (*env)->GetObjectClass(env, object_android);
    jobject j_obj = (*env)->NewGlobalRef(env, object_android);//关键，要不会崩溃
    jmethodID jmid = (*env)->GetMethodID(env, callbackClass, "callBackMessage",
                                         "(I[B)V");
    if (!jmid) {
        LOGE("call back java error,can not find methodId callBackMessage");
        return;
    }

    /* data to jstring*/
    jbyteArray recv_body;
    if (data != NULL) {
        recv_body = (*env)->NewByteArray(env, (jsize) len);
        (*env)->SetByteArrayRegion(env, recv_body, 0, (jsize) len, (jbyte *) data);
    } else {
        recv_body = (*env)->NewByteArray(env, 0);
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
static int callback_data_to_java(void *object_android, int core, const char *data, ssize_t len,
                                 void *user_data) {

    JNIEnv *env;
    (*g_clazz.g_jvm)->AttachCurrentThread(g_clazz.g_jvm, &env, NULL);

    /* find class and get method */
    jclass callbackClass = (*env)->GetObjectClass(env, object_android);
    jobject j_obj = (*env)->NewGlobalRef(env, object_android);//关键，要不会崩溃
    jmethodID jm_id = (*env)->GetMethodID(env, callbackClass, "callBackData",
                                          "(ILjava/lang/String;[B)V");
    if (!jm_id) {
        LOGE("call back error,can not find methodId callBackReadData");
        return -1;
    }

    /* data to jbyteArray*/
    jbyteArray recv_body = (*env)->NewByteArray(env, (jsize) len);
    (*env)->SetByteArrayRegion(env, recv_body, 0, (jsize) len, (jbyte *) data);
    jstring tag = NULL;
    if (user_data) {
        tag = (*env)->NewStringUTF(env, user_data);
    }

    /* call back */
    (*env)->CallVoidMethod(env, j_obj, jm_id, core, tag, recv_body);

    /* free */
    (*env)->DeleteGlobalRef(env, j_obj);
    (*env)->DeleteLocalRef(env, recv_body);
    (*env)->DeleteLocalRef(env, callbackClass);
    (*env)->DeleteLocalRef(env, tag);

    return 0;
}

static jstring getString(JNIEnv *env, jobject param, const char *field) {
    jclass sendParamsClass = (*env)->GetObjectClass(env, param);
    jfieldID jfieldId = (*env)->GetFieldID(env, sendParamsClass, field, "Ljava/lang/String;");
    if (!jfieldId) {
        return NULL;
    }
    jstring string = (jstring) (*env)->GetObjectField(env, param, jfieldId);
    (*env)->DeleteLocalRef(env, sendParamsClass);
    return string;
}

static jint getInt(JNIEnv *env, jobject param, const char *field) {
    jclass sendParamsClass = (*env)->GetObjectClass(env, param);
    jfieldID jfieldId = (*env)->GetFieldID(env, sendParamsClass, field, "I");
    if (!jfieldId) {
        return 0;
    }
    jint data = (*env)->GetIntField(env, param, jfieldId);
    (*env)->DeleteLocalRef(env, sendParamsClass);
    return data;
}

static jbyteArray getByteArray(JNIEnv *env, jobject param, const char *field) {
    jclass sendParamsClass = (*env)->GetObjectClass(env, param);
    jfieldID jfieldId = (*env)->GetFieldID(env, sendParamsClass, field, "[B");
    if (!jfieldId) {
        return 0;
    }
    jbyteArray data = (*env)->GetObjectField(env, param, jfieldId);
    (*env)->DeleteLocalRef(env, sendParamsClass);
    return data;
}


static int build_headers_from_params(JNIEnv *env, jobject param, const char *field,
                                     xqc_cli_http_header_t headers[]) {
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
        if (i > MAX_HEADER) {
            LOGW("set headers error: Exceeding the maximum value of %d ", MAX_HEADER);
            break;
        }
        jobject entryObj = (*env)->CallObjectMethod(env, iteratorObj, nextMID);

        jstring keyString = (*env)->CallObjectMethod(env, entryObj, getKeyMID);
        const char *keyChar = (*env)->GetStringUTFChars(env, keyString, 0);
        int name_len = strlen(keyChar);
        memset(headers[i].name, 0, MAX_HEADER_DATA_LEN);
        if (name_len < MAX_HEADER_DATA_LEN) {
            memcpy(headers[i].name, keyChar, name_len);
            headers[i].name_len = name_len;
        } else {
            LOGW("set header error: Exceeding the maximum value of %d", MAX_HEADER_DATA_LEN);
        }
        (*env)->ReleaseStringUTFChars(env, keyString, keyChar);//release jstring

        jstring valueString = (*env)->CallObjectMethod(env, entryObj, getValueMID);
        const char *valueChar = (*env)->GetStringUTFChars(env, valueString, 0);
        int value_len = strlen(valueChar);
        memset(headers[i].value, 0, MAX_HEADER_DATA_LEN);
        if (value_len < MAX_HEADER_DATA_LEN) {
            memcpy(headers[i].value, valueChar, value_len);
            headers[i].value_len = value_len;
        } else {
            LOGW("set header error: Exceeding the maximum value of %d", MAX_HEADER_DATA_LEN);
        }
        (*env)->ReleaseStringUTFChars(env, valueString, valueChar);//release jstring

        headers[i].flags = 0;

        i++;
        (*env)->DeleteLocalRef(env, entryObj);
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
static xqc_cli_client_args_t *
get_args_params(JNIEnv *env, jobject param, jobject callback, int connType) {
    jobject gl_callback = (*env)->NewGlobalRef(env, callback);

    jbyteArray url = getByteArray(env, param, "url");
    jint urlLen = getInt(env, param, "urlLen");

    jbyteArray content = getByteArray(env, param, "content");
    jint contentLength = getInt(env, param, "contentLength");

    jbyteArray token = getByteArray(env, param, "token");
    jint tokenLen = getInt(env, param, "tokenLen");

    jbyteArray session = getByteArray(env, param, "session");
    jint sessionLen = getInt(env, param, "sessionLen");

    jint alpnType = getInt(env, param, "alpnType");
    jbyteArray alpnName = getByteArray(env, param, "alpnName");
    jint alpnLen = getInt(env, param, "alpnLen");

    jint connectTimeOut = getInt(env, param, "connectTimeOut");
    jint readTimeOut = getInt(env, param, "readTimeOut");
    jint maxRecvDataLen = getInt(env, param, "maxRecvDataLen");
    jint ccType = getInt(env, param, "ccType");
    jint protoVersion = getInt(env, param, "protoVersion");
    jint headersSize = getInt(env, param, "headersSize");
    jint cryptoFlag = getInt(env, param, "cryptoFlag");
    jint finishFlag = getInt(env, param, "finishFlag");
    jint dataType = getInt(env, param, "dataType");

    xqc_cli_client_args_t *args = calloc(1, sizeof(xqc_cli_client_args_t));
    memset(args, 0, sizeof(xqc_cli_client_args_t));

    /* init network config */
    if (connectTimeOut > 0) {
        args->net_cfg.conn_timeout = connectTimeOut;
    } else {
        args->net_cfg.conn_timeout = 30;
        args->net_cfg.read_timeout = readTimeOut;
    }
    args->net_cfg.conn_type = connType;
    args->net_cfg.mode = MODE_SCMR;
    args->net_cfg.cc = ccType;
    args->net_cfg.version = protoVersion;
    args->net_cfg.pacing = 0;

    /* init req config*/
    args->req_cfg.request_cnt = 1;
    args->req_cfg.finish_flag = finishFlag;/* stream is finish */
    if (url != NULL && urlLen > 0) {
        memset(args->req_cfg.urls, 0, URL_LEN);
        (*env)->GetByteArrayRegion(env, url, 0, urlLen, (jbyte *) args->req_cfg.urls);
    }

    /* init env config */
    args->env_cfg.log_level = XQC_LOG_DEBUG;

    /* init quic config */
    args->quic_cfg.alpn_type = alpnType;
    if (alpnType == ALPN_HQ) {
        (*env)->GetByteArrayRegion(env, alpnName, 0, alpnLen, (jbyte *) args->quic_cfg.alpn);
        args->quic_cfg.alpn_len = alpnLen;
    }
    args->quic_cfg.keyupdate_pkt_threshold = UINT16_MAX;
    args->quic_cfg.no_crypto_flag = cryptoFlag;/*set crypto 1:without crypto*/
    if (token != NULL && tokenLen > 0) {
        if (tokenLen < XQC_MAX_TOKEN_LEN) {
            (*env)->GetByteArrayRegion(env, token, 0, tokenLen, (jbyte *) args->quic_cfg.token);
            args->quic_cfg.token_len = tokenLen;
        } else {
            LOGE("token set error : to lang > %d", XQC_MAX_TOKEN_LEN);
        }
    }
    if (session != NULL && sessionLen > 0) {
        if (sessionLen < MAX_SESSION_TICKET_LEN) {
            (*env)->GetByteArrayRegion(env, session, 0, sessionLen,
                                       (jbyte *) args->quic_cfg.session);
            args->quic_cfg.st_len = sessionLen;
            LOGD("session = %s", args->quic_cfg.session);
        } else {
            LOGE("session set error : to lang > %d", MAX_SESSION_TICKET_LEN);
        }
    }

    /* init stream config */
    if (content != NULL) {
        args->user_stream.send_body = malloc(contentLength);
        (*env)->GetByteArrayRegion(env, content, 0, contentLength,
                                   (jbyte *) args->user_stream.send_body);
        args->user_stream.send_body_len = contentLength;
    }
    if (maxRecvDataLen > 0) {
        args->user_stream.recv_body_max_len = maxRecvDataLen;
    } else {
        args->user_stream.recv_body_max_len = MAX_REC_DATA_LEN;
    }

    /* init user config */
    xqc_cli_user_data_params_t *user_params = &args->user_params;

    /* key param */
    user_params->mutex = &g_clazz.mutex;
    user_params->data_type = dataType;

    /* if hq,no header,no need to create header*/
    if (alpnType > 0) {
        /* build header from params */
        if (build_headers_from_params(env, param, "headers", user_params->headers) >= 0) {
            user_params->header_count = headersSize;
        } else {
            LOGE("build_headers_from_params error");
        }
    }

    /* callback */
    user_params->user_data_callback.object_android = gl_callback;
    user_params->user_data_callback.callback_data = callback_data_to_java;
    user_params->user_data_callback.callback_msg = callback_msg_to_java;

    return args;
}


/**
 * 短链接发送
 * @param env
 * @param this
 * @param param
 * @param callback
 * @return
 */
static int short_send(JNIEnv *env, jobject this, jobject param, jobject callback) {
    /* start to send data */
    return client_short_send(get_args_params(env, param, callback, CONN_TYPE_SHORT));
}

/**
 * 短链接取消
 * @param env
 * @param this
 * @param clientCtx
 * @return
 */
static int short_cancel(JNIEnv *env, jobject this, jlong clientCtx) {
    return client_short_cancel(jlong_to_ptr(clientCtx));
}

/**
 * 长链接连接
 * @param env
 * @param this
 * @param param
 * @param callback
 * @return
 */
static long long_connect(JNIEnv *env, jobject this, jobject param, jobject callback) {
    DEBUG;
    xqc_cli_client_args_t *args = get_args_params(env, param, callback, CONN_TYPE_LONG);
    xqc_cli_ctx_t *ctx = client_long_conn(args);
    if (ctx == NULL) {
        return -1;
    }
    return ptr_to_jlong(ctx);
}

/**
 * 长连接开始
 * @param env
 * @param this
 * @param clientCtx
 * @return
 */
static int long_start(JNIEnv *env, jobject this, jlong clientCtx) {
    return client_long_start(jlong_to_ptr(clientCtx));
}

/**
 * 长链接发送ping
 * @param env
 * @param this
 * @param clientCtx
 * @param pingContent
 * @return
 */
static int long_send_ping(JNIEnv *env, jobject this, jlong clientCtx, jbyteArray ping, jint len) {
    const char *cPingContent[MAX_PING_LEN] = {0};
    if (ping != NULL) {
        (*env)->GetByteArrayRegion(env, ping, 0, len, cPingContent);
    }
    return client_long_send_ping(jlong_to_ptr(clientCtx), cPingContent, len);
}


/**
 * send byte
 * @param env
 * @param this
 * @param clientCtx
 * @param content
 * @return
 */
static int
lang_send_byte(JNIEnv *env, jobject this, jlong clientCtx, jint data_type, jobject buffer,
               jint len) {
    const char *cContent = NULL;
    if (buffer != NULL) {
        cContent = (*env)->GetDirectBufferAddress(env, buffer);
    }
    if (cContent == NULL) {
        return -1;
    }
    return client_long_send(jlong_to_ptr(clientCtx), cContent, data_type, len);
}

/**
 * 长连接取消
 * @param env
 * @param this
 * @param clientCtx
 * @return
 */
static int long_cancel(JNIEnv *env, jobject this, jlong clientCtx) {
    return client_long_cancel(jlong_to_ptr(clientCtx));
}


/**
 * 短链接方法映射
 */
static JNINativeMethod g_short_methods[] = {
        {"send",   "(Lcom/lizhi/component/net/xquic/quic/SendParams;Lcom/lizhi/component/net/xquic/quic/XquicCallback;)I", (void *) short_send},
        {"cancel", "(J)I",                                                                                                 (void *) short_cancel},
};

/**
 * 长链接方法映射
 */
static JNINativeMethod g_long_methods[] = {
        {"connect",  "(Lcom/lizhi/component/net/xquic/quic/SendParams;Lcom/lizhi/component/net/xquic/quic/XquicCallback;)J", (void *) long_connect},
        {"start",    "(J)I",                                                                                                 (void *) long_start},
        {"sendPing", "(J[BI)I",                                                                                              (void *) long_send_ping},
        {"sendByte", "(JILjava/nio/ByteBuffer;I)I",                                                                          (void *) lang_send_byte},
        {"cancel",   "(J)I",                                                                                                 (void *) long_cancel},
};

/**
 * jni 加载
 * @param vm
 * @param reserved
 * @return
 */
JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    DEBUG;
    JNIEnv *env = NULL;
    g_clazz.g_jvm = vm;
    if ((*vm)->GetEnv(vm, (void **) &env, JNI_VERSION_1_4) != JNI_OK) {
        return -1;
    }
    assert(env != NULL);

    /* register short methods*/
    XQUIC_FIND_JAVA_CLASS(env, g_clazz.clazz_short, JNI_CLASS_XQUIC_SHORT);
    (*env)->RegisterNatives(env, g_clazz.clazz_short, g_short_methods, NELEM(g_short_methods));

    /* register long methods*/
    XQUIC_FIND_JAVA_CLASS(env, g_clazz.clazz_long, JNI_CLASS_XQUIC_LONG);
    (*env)->RegisterNatives(env, g_clazz.clazz_long, g_long_methods, NELEM(g_long_methods));

    /* init lock */
    pthread_mutex_init(&g_clazz.mutex, NULL);
    return JNI_VERSION_1_4;
}

JNIEXPORT void JNI_OnUnload(JavaVM *jvm, void *reserved) {
    DEBUG;
    pthread_mutex_destroy(&g_clazz.mutex);
}
