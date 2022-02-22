#include "common.h"
#include "native-lib.h"
#include "xquic_client.h"

/**
* xquic 初始化
*/
JNIEXPORT jlong JNICALL Java_com_lizhi_component_net_xquic_native_XquicNative_xquicInit(JNIEnv* env,jobject obj ,jstring host ,int port,jstring token,jstring session){
    if(host == NULL){
        return -1;
    }
    const char* cHost = (*env)->GetStringUTFChars(env,host,0);

    const char* cToken;
    if(token!=NULL){
        cToken = (*env)->GetStringUTFChars(env,token,0);
    }

    const char* cSession = NULL;
    if(session!=NULL){
        cSession = (*env)->GetStringUTFChars(env,session,0);
    }

    client_ctx_t*  ret = init_client(cHost,port,cToken,cSession);
    LOGI("xquicInit host:%s, port:%d, token:%s, session:%s, clinet:%p, engine:%p\n",cHost,port,cToken,cSession,ret,ret->engine);
    return ptr_to_jlong(ret);
}

/**
* 开始
*/
JNIEXPORT jint JNICALL Java_com_lizhi_component_net_xquic_native_XquicNative_xquicStart(JNIEnv *env, jclass cls,jlong clientCtx,jstring content){

    return 0;
}


/**
* 发送数据
*/
JNIEXPORT jint JNICALL Java_com_lizhi_component_net_xquic_native_XquicNative_xquicSend(JNIEnv *env, jclass cls,jlong clientCtx,jint type,jstring content){
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

    LOGI("xquicSend clientCtx:%p, type:%d ,content:%s, engine:%p \n",jlong_to_ptr(clientCtx),type,cContent,ctx_t->engine);
    return 0;
}

/**
* 销毁
*/
JNIEXPORT jint JNICALL Java_com_lizhi_component_net_xquic_native_XquicNative_xquicDestroy(JNIEnv *env, jclass cls,jlong clientCtx,jstring content){

    return 0;
}
