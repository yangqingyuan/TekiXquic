//
// Created by lizhi on 2022/3/23.
//

#ifndef XQUICDEMO_XQUIC_COMMON_H
#define XQUICDEMO_XQUIC_COMMON_H

#include <xquic.h>
#include <xquic_typedef.h>
#include <xqc_http3.h>
#include <stdio.h>
#include <event.h>
#include <memory.h>
#include <sys/socket.h>
#include <unistd.h>
#include <errno.h>
#include <signal.h>
#include <fcntl.h>
#include <stdlib.h>
#include <time.h>
#include <inttypes.h>
#include <netdb.h>
#include <string.h>
#include "common.h"

typedef struct xqc_cli_user_conn_s xqc_cli_user_conn_t;

/* the congestion control types */
typedef enum cc_type_s {
    CC_TYPE_BBR,
    CC_TYPE_CUBIC,
    CC_TYPE_RENO
} CC_TYPE;


/**
 * 数据流
 */
typedef struct xqc_cli_user_stream_s {
    xqc_cli_user_conn_t *user_conn;

    /* stat for IO */
    char *send_body;//发送内容
    size_t send_body_len;//发送的长度
    uint64_t send_offset;//已经发送的内容坐标


    size_t recv_body_len;
    char * recv_body;
    int recv_fin;
    xqc_msec_t start_time;

    /* h3 request content */
    xqc_h3_request_t *h3_request;
    xqc_http_headers_t h3_hdrs;
    uint8_t hdr_sent;
} xqc_cli_user_stream_t;



/**
 * ============================================================================
 * the network config definition section
 * network config is those arguments about socket connection
 * all configuration on network should be put under this section
 * ============================================================================
 */

/**
 * 任务模式
 */
typedef enum xqc_cli_task_mode_s {
    /* send multi requests in single connection with multi streams */
    MODE_SCMR,

    /* serially send multi requests in multi connections, with one request each connection */
    MODE_SCSR_SERIAL,

    /* concurrently send multi requests in multi connections, with one request each connection */
    MODE_SCSR_CONCURRENT,
} xqc_cli_task_mode_t;

/**
 * 网络配置
 */
typedef struct xqc_cli_net_config_s {
    /* server addr info */
    struct sockaddr_in6 addr;
    socklen_t addr_len;
    char server_addr[64];
    short server_port;
    char host[256];

    /* ipv4 or ipv6 */
    int ip_type;

    /* congestion control algorithm */
    CC_TYPE cc;     /* congestion control algorithm */
    int pacing; /* is pacing on */

    /* idle persist timeout */
    int conn_timeout;

    /** 任务模式*/
    xqc_cli_task_mode_t mode;
} xqc_cli_net_config_t;


/**
 * ============================================================================
 * the quic config definition section
 * quic config is those arguments about quic connection
 * all configuration on network should be put under this section
 * ============================================================================
 */
/* definition for quic */
#define MAX_SESSION_TICKET_LEN      2048    /* session ticket len */
#define MAX_TRANSPORT_PARAMS_LEN    2048    /* transport parameter len */
#define XQC_MAX_TOKEN_LEN           256     /* token len */

/**
 * 协议类型
 */
typedef enum xqc_cli_alpn_type_s {
    ALPN_HQ,
    ALPN_H3
} xqc_cli_alpn_type_t;

typedef enum h3_hdr_type {
    /* rsp */
    H3_HDR_STATUS,
    H3_HDR_CONTENT_TYPE,
    H3_HDR_CONTENT_LENGTH,
    H3_HDR_METHOD,
    H3_HDR_SCHEME,
    H3_HDR_HOST,
    H3_HDR_PATH,

    H3_HDR_CNT
} H3_HDR_TYPE;


/**
 *
 */
typedef struct xqc_cli_quic_config_s {
    /*协议类型 */
    xqc_cli_alpn_type_t alpn_type;
    char alpn[16];

    /* 0-rtt config */
    int st_len;                        /* session ticket len */
    char st[MAX_SESSION_TICKET_LEN];    /* session ticket buf */
    int tp_len;                        /* transport params len */
    char tp[MAX_TRANSPORT_PARAMS_LEN];  /* transport params buf */
    int token_len;                     /* token len */
    char token[XQC_MAX_TOKEN_LEN];      /* token buf */

    char *cipher_suites;                /* cipher suites */

    uint8_t use_0rtt;                   /* 0-rtt switch, default turned off */
    uint64_t keyupdate_pkt_threshold;   /* packet limit of a single 1-rtt key, 0 for unlimited */

} xqc_cli_quic_config_t;


/**
 * ============================================================================
 * the environment config definition section
 * environment config is those arguments about IO inputs and outputs
 * all configuration on environment should be put under this section
 * ============================================================================
 */

/**
 * 环境配置
 */
typedef struct xqc_cli_env_config_s {
    /* log path */
    char log_path[256];
    int log_level;

    /* out file */
    char out_file_dir[256];

    /* key export */
    int key_output_flag;
    char key_out_path[256];

    /* life cycle */
    int life;
} xqc_cli_env_config_t;

/**
* ============================================================================
* the request config definition section
* all configuration on request should be put under this section
* ============================================================================
*/
#define MAX_REQUEST_CNT 2048    /* client might deal MAX_REQUEST_CNT requests once */
#define MAX_REQUEST_LEN 256     /* the max length of a request */
#define PATH_LEN            512
#define RESOURCE_LEN        256
#define AUTHORITY_LEN       128
#define URL_LEN             512

#define XQC_INTEROP_TLS_GROUPS  "X25519:P-256:P-384:P-521"

/* request method */
typedef enum request_method_e {
    REQUEST_METHOD_GET,
    REQUEST_METHOD_POST,
} REQUEST_METHOD;


/**
 * 单个请求
 */
typedef struct xqc_cli_request_s {
    char path[RESOURCE_LEN];         /* request path */
    char scheme[8];                  /* request scheme, http/https */
    REQUEST_METHOD method;
    char auth[AUTHORITY_LEN];
    char url[URL_LEN];               /* original url */
    // char            headers[MAX_HEADER][256];   /* field line of h3 */

} xqc_cli_request_t;

/**
 * 多个请求
 */
typedef struct xqc_cli_requests_s {
    /* requests */
    char urls[MAX_REQUEST_CNT * MAX_REQUEST_LEN];
    int request_cnt;    /* requests cnt in urls */
    xqc_cli_request_t reqs[MAX_REQUEST_CNT];
} xqc_cli_requests_t;

/**
 * rev service data back to client
 */
typedef int (*xqc_cli_read_data_callback)(int core, char *data, ssize_t len);

/**
 * user custom （要增加更多的回调给到jni层，可以再这里增加）
 */
typedef struct xqc_cli_user_callback_s {
    xqc_cli_read_data_callback read_data_callback;
} xqc_cli_user_callback_t;

/**
 * ============================================================================
 * the client args definition section
 * client initial args
 * ============================================================================
 */


/**
* 参数，封装了配置
*/
typedef struct xqc_cli_client_args_s {
    /* network args */
    xqc_cli_net_config_t net_cfg;

    /* quic args */
    xqc_cli_quic_config_t quic_cfg;

    /* environment args */
    xqc_cli_env_config_t env_cfg;

    /* request args */
    xqc_cli_requests_t req_cfg;

    /* user stream*/
    xqc_cli_user_stream_t user_stream;

    /* user callback*/
    xqc_cli_user_callback_t user_callback;
} xqc_cli_client_args_t;

/**
 * 任务状态
 */
typedef enum xqc_cli_task_status_s {
    TASK_STATUS_WAITTING,
    TASK_STATUS_RUNNING,
    TASK_STATUS_FINISHED,
    TASK_STATUS_FAILED,
} xqc_cli_task_status_t;

/**
 * 任务调度信息
 */
typedef struct xqc_cli_task_schedule_info_s {
    xqc_cli_task_status_t status;         /* task status */
    int req_create_cnt; /* streams created */
    int req_sent_cnt;
    int req_fin_cnt;    /* the req cnt which have received FIN */
    uint8_t fin_flag;       /* all reqs finished, need close */
} xqc_cli_task_schedule_info_t;

/**
 * the task schedule info, used to mark the operation
 * info of all requests, the client will exit when all
 * tasks are finished or closed
 *
 */
typedef struct xqc_cli_task_schedule_s {
    /* the cnt of tasks that been running or have been ran */
    int idx;

    /* the task status, 0: not executed; 1: suc; -1: failed */
    xqc_cli_task_schedule_info_t *schedule_info;
} xqc_cli_task_schedule_t;


/*
 * task info structure.
 * a task is strongly correlate to a net connection
 */
typedef struct xqc_cli_task_s {
    int task_idx;
    int req_cnt;
    xqc_cli_request_t *reqs;      /* a task could contain multipule requests, which wil be sent  */
    xqc_cli_user_conn_t *user_conn; /* user_conn handle */
} xqc_cli_task_t;

/**
 * 任务上下文
 */
typedef struct xqc_cli_task_ctx_s {
    /* task mode */
    xqc_cli_task_mode_t mode;

    /* total task cnt */
    int task_cnt;

    /* task list */
    xqc_cli_task_t *tasks;

    /* current task schedule info */
    xqc_cli_task_schedule_t schedule; /* current task index */
} xqc_cli_task_ctx_t;


/***
 * client 上下文
 */
typedef struct xqc_cli_ctx_s {
    /* xquic engine context */
    xqc_engine_t *engine;

    /* libevent context */
    struct ev_timer ev_engine;
    struct ev_async ev_task;
    struct ev_timer ev_kill;
    struct ev_loop *eb;  /* handle of libevent */

    /* log context */
    int log_fd;
    char log_path[256];

    /* key log context */
    int keylog_fd;

    /* client context */
    xqc_cli_client_args_t *args;

    /* task schedule context */
    xqc_cli_task_ctx_t task_ctx;
} xqc_cli_ctx_t;


/***
 *
 */
typedef struct xqc_cli_user_conn_s {
    int fd;
    xqc_cid_t cid;

    struct sockaddr_in6 local_addr;
    socklen_t local_addrlen;

    struct ev_io ev_socket;
    struct ev_timer ev_timeout;

    xqc_cli_ctx_t *ctx;
    uint64_t last_sock_op_time;
    xqc_cli_task_t *task;

} xqc_cli_user_conn_t;


inline uint64_t xqc_now() {
    /* get microsecond unit time */
    struct timeval tv;
    gettimeofday(&tv, NULL);
    uint64_t ul = tv.tv_sec * (uint64_t) 1000000 + tv.tv_usec;
    return ul;
}


#endif //XQUICDEMO_XQUIC_COMMON_H
