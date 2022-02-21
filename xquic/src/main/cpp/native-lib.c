#include <jni.h>
#include <log.h>
#include "native-lib.h"
#include "xquic_client.h"


JNIEXPORT void JNICALL Java_com_lizhi_component_net_xquic_native_XquicNative_init(JNIEnv* env,jobject obj,jstring host ,int port) {
    LOGE("init");
    initEngine();
}


JNIEXPORT void JNICALL Java_com_lizhi_component_net_xquic_native_XquicNative_send(JNIEnv* env,jobject obj ,jstring content) {
    LOGE("send");
}