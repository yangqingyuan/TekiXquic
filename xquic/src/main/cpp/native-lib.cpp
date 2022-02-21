#include <jni.h>
#include <string>
#include "log.h"

extern "C" JNIEXPORT void JNICALL
Java_com_lizhi_component_net_xquic_native_XquicNative_init(
        JNIEnv* env,
        jobject /* this */ ,jstring host ,int port) {
        LOGE("init");
}


extern "C" JNIEXPORT void JNICALL
Java_com_lizhi_component_net_xquic_native_XquicNative_send(
        JNIEnv* env,
        jobject /* this */ ,jstring content) {
    LOGE("send");
}