#include "common.h"
#include "native-lib.h"
#include "xquic_client.h"

/**
* xquic 初始化
*/
JNIEXPORT jlong JNICALL Java_com_lizhi_component_net_xquic_native_XquicNative_xquicInit(JNIEnv* env,jobject obj){
    client_ctx_t*  ret = client_init();//初始化client
    if(ret == NULL){
        return -1;
    }
    LOGI("xquicInit client:%p, engine:%p,type = %d\n",ret,ret->engine);
    return ptr_to_jlong(ret);
}

/**
* 开始
*/
JNIEXPORT jint JNICALL Java_com_lizhi_component_net_xquic_native_XquicNative_xquicConnect(JNIEnv *env, jclass cls,jlong clientCtx,jstring host,
    int port,jstring token,jstring session){
    DEBUG;
    if(host == NULL){
        LOGE("xquicConnect error host == NULL");
        return -1;
    }
    const char* cHost = (*env)->GetStringUTFChars(env,host,0);

    const char* cToken = NULL;
    if(token!=NULL){
        cToken = (*env)->GetStringUTFChars(env,token,0);
    }

    const char* cSession = NULL;
    if(session!=NULL){
        cSession = (*env)->GetStringUTFChars(env,session,0);
    }

    if(clientCtx == NULL){
       LOGE("xquicConnect error clientCtx == NULL");
       return -1;
    }

    client_ctx_t*  ctx_t = (client_ctx_t* )jlong_to_ptr(clientCtx);

    LOGI("xquicConnect host:%s, port:%d, token:%s, session:%s, client:%p, engine:%p\n",cHost,port,cToken,cSession,ctx_t,ctx_t->engine);

    int ret = client_connect(ctx_t,cHost,port,cToken,cSession,NULL);//开始链接

    (*env)->ReleaseStringUTFChars(env, host, cHost);
    (*env)->DeleteLocalRef(env, host);

    if(session!=NULL){
        (*env)->ReleaseStringUTFChars(env, session, cSession);
        (*env)->DeleteLocalRef(env, session);
    }

    if(token!=NULL){
        (*env)->ReleaseStringUTFChars(env, token, cToken);
        (*env)->DeleteLocalRef(env, token);
    }

    return ret;
}

/**
* 开始,其实就是开始looper，会阻塞线程，上层需要起一个线程来启动
*/
JNIEXPORT void JNICALL Java_com_lizhi_component_net_xquic_native_XquicNative_xquicStart
        (JNIEnv *env, jclass cls,jlong clientCtx){
         client_ctx_t*  ctx_t = (client_ctx_t* )jlong_to_ptr(clientCtx);
         client_start(ctx_t);
}

#define MAX_HEADER 100
/**
* 发送数据
*/
JNIEXPORT jint JNICALL Java_com_lizhi_component_net_xquic_native_XquicNative_xquicH3Get(JNIEnv *env, jclass cls,jlong clientCtx,jstring content){
    DEBUG;
    if(content == NULL){
        LOGE("xquicSend error content == NULL");
        return -1;
    }
    const char* cContent = (*env)->GetStringUTFChars(env,content,0);
    if(clientCtx == NULL){
        LOGE("xquicSend error clientCtx == NULL");
        return -1;
    }
    client_ctx_t*  ctx_t = (client_ctx_t* )jlong_to_ptr(clientCtx);

    LOGI("xquicSend clientCtx:%p,  engine:%p, content:%s,\n",clientCtx,ctx_t->engine,cContent);

    int header_size = 6;

    xqc_http_header_t header[MAX_HEADER] = {
        {
            .name   = {.iov_base = ":method", .iov_len = 7},
            .value  = {.iov_base = "POST", .iov_len = 4},
            .flags  = 0,
        },
        {
            .name   = {.iov_base = ":scheme", .iov_len = 7},
            .value  = {.iov_base = "https", .iov_len = strlen("https")},
            .flags  = 0,
        },
        {
            .name   = {.iov_base = "host", .iov_len = 4},
            .value  = {.iov_base = "com.xx", .iov_len = strlen("com.xx")},
            .flags  = 0,
        },
        {
            .name   = {.iov_base = ":path", .iov_len = 5},
            .value  = {.iov_base = "test", .iov_len = strlen("test")},
            .flags  = 0,
        },
        {
            .name   = {.iov_base = "content-type", .iov_len = 12},
            .value  = {.iov_base = "text/plain", .iov_len = 10},
            .flags  = 0,
        },
        {
            .name   = {.iov_base = "content-length", .iov_len = 14},
            .value  = {.iov_base = 0, .iov_len = 0},
            .flags  = 0,
        },
    };
    xqc_http_headers_t headers = {
            .headers = header,
            .count  = header_size,
        };


    header[0].value.iov_base = "GET";
    header[0].value.iov_len = sizeof("GET") - 1;

    client_send_h3_get(ctx_t,&headers);//发送头

    (*env)->ReleaseStringUTFChars(env, content, cContent);
    (*env)->DeleteLocalRef(env, content);
    return 0;
}

/**
* H3的方式发送数据
*/
JNIEXPORT jint JNICALL Java_com_lizhi_component_net_xquic_native_XquicNative_xquicH3Post(JNIEnv *env, jclass cls,jlong clientCtx,jstring content){

}

/**
* 销毁
*/
JNIEXPORT jint JNICALL Java_com_lizhi_component_net_xquic_native_XquicNative_xquicDestroy(JNIEnv *env, jclass cls,jlong clientCtx){
    client_ctx_t*  ctx_t = (client_ctx_t* )jlong_to_ptr(clientCtx);
    LOGI("xquicDestroy clientCtx:%p,  engine:%p \n",clientCtx,ctx_t->engine);

    return 0;
}
