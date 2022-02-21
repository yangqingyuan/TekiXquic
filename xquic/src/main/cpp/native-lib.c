#include <jni.h>
#include "common.h"
#include "native-lib.h"
#include "xquic_client.h"

/**
* xquic 初始化
*/
JNIEXPORT long JNICALL Java_com_lizhi_component_net_xquic_native_XquicNative_xquicInit(JNIEnv* env,jobject obj ,jstring host ,int port,jstring token,jstring session){

    return init_client();
}

/**
* 开始
*/
JNIEXPORT int JNICALL Java_com_lizhi_component_net_xquic_native_XquicNative_xquicStart(JNIEnv *env, jclass cls,long clientCtx,jstring content){

    return 0;
}


/**
* 发送数据
*/
JNIEXPORT int JNICALL Java_com_lizhi_component_net_xquic_native_XquicNative_xquicSend(JNIEnv *env, jclass cls,long clientCtx,jstring content){
    return 0;
}

/**
* 销毁
*/
JNIEXPORT int JNICALL Java_com_lizhi_component_net_xquic_native_XquicNative_xquicDestroy(JNIEnv *env, jclass cls,long clientCtx,jstring content){
    return 0;
}
