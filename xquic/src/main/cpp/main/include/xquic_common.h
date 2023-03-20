//
// Created by yqy on 2022/3/23.
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
#include <cJSON.h>
#include <pthread.h>
#include <xquic_msg_queue.h>


typedef struct xqc_cli_user_conn_s xqc_cli_user_conn_t;

#define MAX_REC_DATA_LEN           1024*1024     /* recv data max len */
#define XQC_PACKET_TMP_BUF_LEN     1500
#define MAX_SEND_DATA_LEN          1024*512

/* the congestion control types */
typedef enum cc_type_s {
    CC_TYPE_BBR,
    CC_TYPE_CUBIC,
    CC_TYPE_RENO
} CC_TYPE;


/* the conn types */
typedef enum conn_type_s {
    CONN_TYPE_SHORT,
    CONN_TYPE_LONG,
} CONN_TYPE;

/**
 * cmd type（指令类型）
 */
typedef enum cmd_type_s {
    CMD_TYPE_NONE,
    CMD_TYPE_INIT_TASK,
    CMD_TYPE_SEND_PING,
    CMD_TYPE_SEND_DATA,
    CMD_TYPE_CANCEL,//disconnect
    CMD_TYPE_DESTROY,//destroy
} CMD_TYPE;


/**
 * msg type
 */
typedef enum msg_type_s {
    MSG_TYPE_INIT,//init
    MSG_TYPE_HANDSHAKE,//init
    MSG_TYPE_TOKEN,//token
    MSG_TYPE_SESSION,//session
    MSG_TYPE_TP,//tp
    MSG_TYPE_HEAD,//head
    MSG_TYPE_PING,//ping data
    MSG_TYPE_DESTROY
} MSG_TYPE;

/**
 * send data type
 */
typedef enum send_data_type_s {
    DATA_TYPE_JSON = 0,
    DATA_TYPE_BYTE = 1
} send_data_type_t;

/**
 * 数据流
 */
typedef struct xqc_cli_user_stream_s {
    xqc_cli_user_conn_t *user_conn;

    /* stat for IO */
    unsigned char *send_body;//发送内容
    size_t send_body_len;//发送的长度
    uint64_t send_offset;//已经发送的内容坐标

    char user_tag[512];//用户标签

    size_t recv_body_len;
    char *recv_body;
    size_t recv_body_max_len;
    int recv_fin;
    xqc_msec_t start_time;

    xqc_stream_t *hq_request;

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

    /* proto version */
    xqc_proto_version_t version;

    /* ipv4 or ipv6 */
    int ip_type;

    /* congestion control algorithm */
    CC_TYPE cc;

    /* is pacing on */
    int pacing;

    /* idle persist timeout */
    int conn_timeout;
    int read_timeout;

    /** 任务模式*/
    xqc_cli_task_mode_t mode;

    /* conn type */
    CONN_TYPE conn_type;
} xqc_cli_net_config_t;


/**
 * ============================================================================
 * the quic config definition section
 * quic config is those arguments about quic connection
 * all configuration on network should be put under this section
 * ============================================================================
 */
/* definition for quic */
#define MAX_SESSION_TICKET_LEN      1024*10    /* session ticket len */
#define MAX_TRANSPORT_PARAMS_LEN    2048   /* transport parameter len */
#define XQC_MAX_TOKEN_LEN           256*10     /* token len */

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
    int alpn_len;

    /* 0-rtt config */
    size_t st_len;                        /* session ticket len */
    const unsigned char session[MAX_SESSION_TICKET_LEN];    /* session ticket buf */
    int tp_len;                        /* transport params len */
    char tp[MAX_TRANSPORT_PARAMS_LEN];  /* transport params buf */
    size_t token_len;                     /* token len */
    const unsigned char token[XQC_MAX_TOKEN_LEN];      /* token buf */

    char *cipher_suites;                /* cipher suites */

    uint8_t no_crypto_flag;             /* 1:without crypto */

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
    //char log_path[256];
    int log_level;

    /* out file */
    //char out_file_dir[256];

    /* key export */
    int key_output_flag;
    char key_out_path[256];

    /* life cycle TODO 目前没有用到*/
    int life;
} xqc_cli_env_config_t;

/**
* ============================================================================
* the request config definition section
* all configuration on request should be put under this section
* ============================================================================
*/
#define MAX_REQUEST_CNT 10    /* client might deal MAX_REQUEST_CNT requests once */
#define MAX_REQUEST_LEN 256     /* the max length of a request */
#define PATH_LEN            512
#define RESOURCE_LEN        256
#define AUTHORITY_LEN       128
#define URL_LEN             256
#define MAX_HEADER          30
#define MAX_HEADER_DATA_LEN 100
#define MAX_PING_LEN        256

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
    //char path[RESOURCE_LEN];         /* request path */
    //char scheme[8];                  /* request scheme, http/https */
    //REQUEST_METHOD method;
    //char auth[AUTHORITY_LEN];
    //char url[URL_LEN];               /* original url */
    //char headers[MAX_HEADER][256];   /* field line of h3 */
    xqc_http_header_t headers[MAX_HEADER];
    int count;
} xqc_cli_request_t;

/**
 * 多个请求
 */
typedef struct xqc_cli_requests_s {
    /* requests */
    char urls[URL_LEN];
    int request_cnt;    /* requests cnt in urls */
    int finish_flag;     /* request finish flag, 1 for finish. */
    xqc_cli_request_t reqs[MAX_REQUEST_CNT];
} xqc_cli_requests_t;

/**
 * rev service data back to client
 * android
 */
typedef int (*xqc_cli_callback_data)(void *jclass, int core, const char *data,
                                     ssize_t len, void *user_data, int finish);

typedef void (*xcc_cli_callback_msg)(void *jclass, MSG_TYPE msg_type, const char *data,
                                     uint32_t data_len, void *user_data);

typedef struct xqc_cli_user_data_callback_s {
    /* android */
    void *object_android;

    /**
     * callback to client to save
     */
    xqc_cli_callback_data callback_data;

    /**
     * callback to client to save
     */
    xcc_cli_callback_msg callback_msg;

    /* ios */
    //FIXME
} xqc_cli_user_data_callback_t;

typedef struct xqc_cli_http_header_s {
    /* name of http header */
    char name[MAX_HEADER_DATA_LEN];
    size_t name_len;

    /* value of http header */
    char value[MAX_HEADER_DATA_LEN];
    size_t value_len;

    uint8_t flags;

} xqc_cli_http_header_t;

/**
 * user custom （要增加更多的回调给到jni层，可以再这里增加）
 */
typedef struct xqc_cli_user_data_params_s {

    xqc_cli_user_data_callback_t user_data_callback;

    int data_type;

    pthread_mutex_t *mutex;

    /* headers */
    xqc_cli_http_header_t headers[MAX_HEADER];
    int header_count;

    /* proto version */
    xqc_proto_version_t version;

    /* alpn type */
    xqc_cli_alpn_type_t alpn_type;
} xqc_cli_user_data_params_t;

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

    /* user config*/
    xqc_cli_user_data_params_t user_params;
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

/**
 * user msg data,long conn user
 */
typedef struct xqc_cli_user_data_msg_s {
    /* msg type */
    CMD_TYPE cmd_type;

    /* cache ping data */
    char ping_data[MAX_PING_LEN];
    int ping_len;

    /* queue to cache send data */
    xqc_cli_message_queue_t message_queue;
} xqc_cli_user_data_msg_t;

/***
 * client 上下文
 */
typedef struct xqc_cli_ctx_s {

    /* is active 1:yes 0 not */
    int active;

    /* xquic engine context */
    xqc_engine_t *engine;

    /* libevent context */
    struct ev_timer ev_engine;
    struct ev_async ev_task;
    struct ev_timer ev_kill;
    struct ev_loop *eb;  /* handle of libevent */

    pthread_mutex_t *mutex;

    /* log context */
    int log_fd;
    char log_path[256];

    /* key log context */
    int keylog_fd;

    /* client context */
    xqc_cli_client_args_t *args;

    /* task schedule context */
    xqc_cli_task_ctx_t task_ctx;

    /* long conn use */
    xqc_cli_user_data_msg_t msg_data;
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
    uint64_t last_sock_write_time;
    uint64_t last_sock_read_time;
    xqc_cli_task_t *task;

} xqc_cli_user_conn_t;


/**
 * call back msg to client(Android/IOS)
 * @param user_conn
 * @param msg_type
 * @param data
 * @param data_len
 */
inline void callback_msg_to_client(xqc_cli_client_args_t *args, MSG_TYPE msg_type,
                                   const char *data,
                                   unsigned data_len) {

    xqc_cli_user_data_params_t *user_params = &(args->user_params);

    /* callback to client */
    if (user_params) {
        user_params->user_data_callback.callback_msg(
                user_params->user_data_callback.object_android, msg_type, data,
                data_len, NULL);
    }
}


/**
 * call back data to client
 * @param core XQC_OK(0) success other fail
 * @param user_conn
 * @param errMsg
 */
inline void
callback_data_to_client(xqc_cli_user_conn_t *user_conn, int core, char *data, size_t len,
                        void *user_data, int isFinish) {
    xqc_cli_user_data_params_t *user_params = &(user_conn->ctx->args->user_params);
    if (user_params) {
        user_params->user_data_callback.callback_data(
                user_params->user_data_callback.object_android, core,
                data, len, user_data, isFinish);
    }
}

/**
 * call back data to client
 * @param user_
 * @param core
 * @param err_msg
 */
inline void
callback_data_to_client_2(xqc_cli_user_data_params_t *user_params, int core, char *data, int isFinish) {
    if (user_params && data != NULL) {
        user_params->user_data_callback.callback_data(
                user_params->user_data_callback.object_android, core,
                data, strlen(data), NULL,isFinish);
    }
}

inline uint64_t xqc_now() {
    /* get microsecond unit time */
    struct timeval tv;
    gettimeofday(&tv, NULL);
    uint64_t ul = tv.tv_sec * (uint64_t) 1000000 + tv.tv_usec;
    return ul;
}

#endif //XQUICDEMO_XQUIC_COMMON_H
