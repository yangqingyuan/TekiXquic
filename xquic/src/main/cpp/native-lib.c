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
* 发送数据
*/
JNIEXPORT jint JNICALL Java_com_lizhi_component_net_xquic_native_XquicNative_xquicSend(JNIEnv *env, jclass cls,jlong clientCtx,jstring content){
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

    (*env)->ReleaseStringUTFChars(env, content, cContent);
    (*env)->DeleteLocalRef(env, content);
    return 0;
}

/**
* H3的方式发送数据
*/
JNIEXPORT jint JNICALL Java_com_lizhi_component_net_xquic_native_XquicNative_xquicH3Send(JNIEnv *env, jclass cls,jlong clientCtx,jstring content){

}

/**
* 销毁
*/
JNIEXPORT jint JNICALL Java_com_lizhi_component_net_xquic_native_XquicNative_xquicDestroy(JNIEnv *env, jclass cls,jlong clientCtx){
    client_ctx_t*  ctx_t = (client_ctx_t* )jlong_to_ptr(clientCtx);
    LOGI("xquicDestroy clientCtx:%p,  engine:%p \n",clientCtx,ctx_t->engine);

    ctx_t->ev_engine.repeat = 1;//单位秒
    ev_timer_again (ctx_t->loop, &ctx_t->ev_engine);//重新设置重复时间，每次调用会覆盖之前的时间，时间开始时间为当前时间
    return 0;
}
