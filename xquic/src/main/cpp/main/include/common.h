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

#define XQC_ALPN_TRANSPORT "transport"

#define XQC_MAX_LOG_LEN 2048
#define XQC_MAX_TOKEN_LEN 256
#define XQC_PACKET_TMP_BUF_LEN 1500

//全局变量，整个xquic只用一个
struct ev_loop *loop;


typedef struct user_conn_s {
    int                 fd;
    xqc_cid_t           cid;

    struct sockaddr    *local_addr;
    socklen_t           local_addrlen;
    xqc_flag_t          get_local_addr;
    struct sockaddr    *peer_addr;
    socklen_t           peer_addrlen;

    unsigned char      *token;
    unsigned            token_len;

    struct ev_io       ev_socket;
    struct ev_timer    ev_timeout;

    int                 h3;
} user_conn_t;

typedef struct user_stream_s {
    xqc_stream_t       *stream;
    xqc_h3_request_t   *h3_request;
    user_conn_t        *user_conn;
    uint64_t            send_offset;
    int                 header_sent;
    int                 header_recvd;
    char               *send_body;
    size_t              send_body_len;
    size_t              send_body_max;
    char               *recv_body;
    size_t              recv_body_len;
    FILE               *recv_body_fp;
    int                 recv_fin;
    xqc_msec_t          start_time;
    xqc_msec_t          first_frame_time;   /* first frame download time */
    xqc_msec_t          last_read_time;
    int                 abnormal_count;
    int                 body_read_notify_cnt;
} user_stream_t;


typedef struct client_ctx_s {
    xqc_engine_t      *engine;
    user_conn_t        *user_conn;
    struct ev_timer   ev_engine;
    struct ev_timer   ev_delay;
} client_ctx_t;


#endif /* _LZ_KEEPLIVE_COMMON_H */