#ifndef _LZ_XNET_COMMON_H
#define _LZ_XNET_COMMON_H

#include <jni.h>
#include <android/log.h>
#include <time.h>
#include <string.h>
#include <memory.h>
#include <sys/socket.h>
#include <sys/syscall.h> /*必须引用这个文件 */
#include <unistd.h>
#include <errno.h>
#include <signal.h>
#include <fcntl.h>
#include <stdlib.h>
#include <stdio.h>
#include <time.h>
#include <inttypes.h>
#include <sys/ioctl.h>
#include <net/if.h>
#include <stdio.h>

#include "ev.h"
#include "event.h"
#include "xquic.h"
#include "xqc_http3.h"
#include "xqc_errno.h"
#include "xquic_typedef.h"

#define DEBUG LOGI("fun:%s,line %d \n", __FUNCTION__, __LINE__);


#ifdef _LP64
#define jlong_to_ptr(a) ((void*)(a))
#define ptr_to_jlong(a) ((jlong)(a))
#else
#define jlong_to_ptr(a) ((void*)(int)(a))
#define ptr_to_jlong(a) ((jlong)(int)(a))
#endif
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