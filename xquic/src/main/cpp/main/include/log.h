#ifndef _LZ_XNET_COMMON_H
#define _LZ_XNET_COMMON_H

#include <jni.h>
#include <android/log.h>
#include <time.h>
#include <string.h>

#define TAG    "LzXquic->jni"
#define LOGW(...)    __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)
#define LOGE(...)    __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define LOGI(...)    __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#ifdef LIB_DEBUG
#define LOGD(...)	__android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#else
#define LOGD(...)
#endif

#endif /* _LZ_KEEPLIVE_COMMON_H */