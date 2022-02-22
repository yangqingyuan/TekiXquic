#include "xquic_client.h"
#include "engine_callbacks.h"
#include "transport_callbacks.h"
#include "h3_callbacks.h"
#include "app_proto_callbacks.h"


/**
* 初始化引擎
*/
xqc_engine_t* init_engine(void *user_data){
    DEBUG;

    //第二步：初始化ssl_config
    xqc_engine_ssl_config_t  engine_ssl_config;
    memset(&engine_ssl_config, 0, sizeof(engine_ssl_config));
    engine_ssl_config.ciphers = XQC_TLS_CIPHERS;//加密算法，用默认
    engine_ssl_config.groups = XQC_TLS_GROUPS;//加密组，默认就行

    //第四步：初始化引擎回调跟传输回调
    xqc_engine_callback_t callback = {
            .set_event_timer = xqc_client_set_event_timer,/* call xqc_engine_main_logic when the timer expires */
            .log_callbacks = {
                .xqc_log_write_err = xqc_client_write_log,
                .xqc_log_write_stat = xqc_client_write_log,
            },
            .keylog_cb = xqc_keylog_cb,
        };

    xqc_transport_callbacks_t tcbs = {
        .write_socket = xqc_client_write_socket,
        .save_token = xqc_client_save_token,
        .save_session_cb = save_session_cb,
        .save_tp_cb = save_tp_cb,
        .cert_verify_cb = xqc_client_cert_verify,
    };

    //第五步：引擎配置
    xqc_config_t config;
    if (xqc_engine_get_default_config(&config, XQC_ENGINE_CLIENT) < 0) {
        LOGE("get default config error fun:%s,line:%d \n",__FUNCTION__,__LINE__);
        return NULL;
    }
    config.cfg_log_level = XQC_LOG_DEBUG;//设置log级别

    //第六部：创建引擎
    return xqc_engine_create(XQC_ENGINE_CLIENT, &config, &engine_ssl_config,&callback, &tcbs,user_data);
}

int init_alpn_ctx(client_ctx_t* ctx){
    DEBUG;
    xqc_h3_callbacks_t h3_cbs = {
        .h3c_cbs = {
            .h3_conn_create_notify = xqc_client_h3_conn_create_notify,
            .h3_conn_close_notify = xqc_client_h3_conn_close_notify,
            .h3_conn_handshake_finished = xqc_client_h3_conn_handshake_finished,
            .h3_conn_ping_acked = xqc_client_h3_conn_ping_acked_notify,
        },
        .h3r_cbs = {
            .h3_request_close_notify = xqc_client_request_close_notify,
            .h3_request_read_notify = xqc_client_request_read_notify,
            .h3_request_write_notify = xqc_client_request_write_notify,
        }
    };

    /* init http3 context */
    int ret = xqc_h3_ctx_init(ctx->engine, &h3_cbs);
    if (ret != XQC_OK) {
        LOGE("init h3 context error, ret: %d fun:%s,line:%d \n", ret,__FUNCTION__,__LINE__);
        return ret;
    }

    /* register transport callbacks */
    xqc_app_proto_callbacks_t ap_cbs = {
        .conn_cbs = {
            .conn_create_notify = xqc_client_conn_create_notify,
            .conn_close_notify = xqc_client_conn_close_notify,
            .conn_handshake_finished = xqc_client_conn_handshake_finished,
            .conn_ping_acked = xqc_client_conn_ping_acked_notify,
        },
        .stream_cbs = {
            .stream_write_notify = xqc_client_stream_write_notify,
            .stream_read_notify = xqc_client_stream_read_notify,
            .stream_close_notify = xqc_client_stream_close_notify,
        }
    };

    return xqc_engine_register_alpn(ctx->engine, XQC_ALPN_TRANSPORT, 9, &ap_cbs);
}

static void timer_action(struct ev_loop *main_loop,ev_timer*time_w,int e){
     LOGE("timer_action %f, %d",time_w->repeat,e);
}


void
xqc_convert_addr_text_to_sockaddr(int type,
    const char *addr_text, unsigned int port,
    struct sockaddr **saddr, socklen_t *saddr_len)
{
    if (type == AF_INET6) {
        *saddr = calloc(1, sizeof(struct sockaddr_in6));
        memset(*saddr, 0, sizeof(struct sockaddr_in6));
        struct sockaddr_in6 *addr_v6 = (struct sockaddr_in6 *)(*saddr);
        inet_pton(type, addr_text, &(addr_v6->sin6_addr.s6_addr));
        addr_v6->sin6_family = type;
        addr_v6->sin6_port = htons(port);
        *saddr_len = sizeof(struct sockaddr_in6);

    } else {
        *saddr = calloc(1, sizeof(struct sockaddr_in));
        memset(*saddr, 0, sizeof(struct sockaddr_in));
        struct sockaddr_in *addr_v4 = (struct sockaddr_in *)(*saddr);
        inet_pton(type, addr_text, &(addr_v4->sin_addr.s_addr));
        addr_v4->sin_family = type;
        addr_v4->sin_port = htons(port);
        *saddr_len = sizeof(struct sockaddr_in);
    }
}



static int
xqc_client_create_socket(int type,
    const struct sockaddr *saddr, socklen_t saddr_len)
{
    int size;
    int fd = -1;

    /* create fd & set socket option */
    fd = socket(type, SOCK_DGRAM, 0);
    if (fd < 0) {
        LOGE("create socket failed, errno: %d\n", errno);
        return -1;
    }

    if (fcntl(fd, F_SETFL, O_NONBLOCK) == -1) {
        LOGE("set socket nonblock failed, errno: %d\n", errno);
        goto err;
    }

    size = 1 * 1024 * 1024;
    if (setsockopt(fd, SOL_SOCKET, SO_RCVBUF, &size, sizeof(int)) < 0) {
        LOGE("setsockopt failed, errno: %d\n", errno);
        goto err;
    }

    if (setsockopt(fd, SOL_SOCKET, SO_SNDBUF, &size, sizeof(int)) < 0) {
        LOGE("setsockopt failed, errno: %d\n", errno);
        goto err;
    }

    /* connect to peer addr */
#if !defined(__APPLE__)

    if (connect(fd, (struct sockaddr *)saddr, saddr_len) < 0) {
        LOGE("connect socket failed, errno: %d\n", errno);
        goto err;
    }
#endif

    return fd;

err:
    close(fd);
    return -1;
}

void
xqc_client_socket_event_callback(struct ev_loop *main_loop,ev_io*io_w,int what)
{
    //DEBUG;
    user_conn_t *user_conn = (user_conn_t *) io_w->user_data;
    /*
    if (what & EV_WRITE) {
        xqc_client_socket_write_handler(user_conn);
    } else if (what & EV_READ) {
        xqc_client_socket_read_handler(user_conn);
    } else {
        LOGE("event callback: what=%d\n", what);
    }*/
}

void
xqc_client_init_addr(user_conn_t *user_conn,
    const char *server_addr, int server_port,int ip_type)
{

    xqc_convert_addr_text_to_sockaddr(ip_type,
                                      server_addr, server_port,
                                      &user_conn->peer_addr,
                                      &user_conn->peer_addrlen);

    if (ip_type == AF_INET6) {
        user_conn->local_addr = (struct sockaddr *)calloc(1, sizeof(struct sockaddr_in6));
        memset(user_conn->local_addr, 0, sizeof(struct sockaddr_in6));
        user_conn->local_addrlen = sizeof(struct sockaddr_in6);

    } else {
        user_conn->local_addr = (struct sockaddr *)calloc(1, sizeof(struct sockaddr_in));
        memset(user_conn->local_addr, 0, sizeof(struct sockaddr_in));
        user_conn->local_addrlen = sizeof(struct sockaddr_in);
    }
}

void xqc_client_socket_read_handler(user_conn_t *user_conn)
{
    DEBUG;
    ssize_t recv_size = 0;
    ssize_t recv_sum = 0;

}

void xqc_client_socket_write_handler(user_conn_t *user_conn){
    DEBUG;

}

void xqc_client_timeout_callback(struct ev_loop *main_loop,ev_timer*time_w,int what){
    //DEBUG;

}

user_conn_t *
xqc_client_user_conn_create(client_ctx_t* ctx,const char *server_addr, int server_port)
{
    user_conn_t *user_conn = calloc(1, sizeof(user_conn_t));

    user_conn->ev_timeout.user_data = user_conn;
    ev_timer_init(&user_conn->ev_timeout, xqc_client_timeout_callback, 0,1);//一秒后超时
    ev_timer_start(ctx->loop,&user_conn->ev_timeout);

    int ip_type = AF_INET; //(g_ipv6 ? AF_INET6 : AF_INET);
    xqc_client_init_addr(user_conn, server_addr, server_port,ip_type);

    user_conn->fd = xqc_client_create_socket(ip_type,
                                             user_conn->peer_addr, user_conn->peer_addrlen);
    if (user_conn->fd < 0) {
        LOGE("xqc_create_socket error server_addr=%s ,server_port=%d\n",server_addr,server_port);
        ev_timer_stop(ctx->loop,&user_conn->ev_timeout);
        return NULL;
    }

    user_conn->ev_socket.user_data = user_conn;
    ev_io_init(&user_conn->ev_socket,xqc_client_socket_event_callback,user_conn->fd,EV_READ | EV_PERSIST);
    ev_io_start(ctx->loop,&user_conn->ev_socket);

    return user_conn;
}

static void
xqc_client_engine_callback(struct ev_loop *main_loop,ev_timer*time_w,int what)
{
    LOGE("timer wakeup %f\n", time_w->repeat);
    //if(time_w->repeat>0){
        client_ctx_t *ctx = (client_ctx_t *) ev_userdata(main_loop);
        LOGE("BBB %p",ctx);
        //xqc_engine_main_logic(ctx->engine);
    //}
}

//初始化client端
client_ctx_t * client_init(){
    DEBUG;
    //初始化clientCtx
    client_ctx_t *ctx = calloc(1, sizeof(client_ctx_t));
    LOGE("AAA %p",ctx);
    //初始化looper
    ctx->loop = EV_DEFAULT;
    ev_set_userdata(ctx->loop,ctx);
    ev_timer_init(&ctx->ev_engine,xqc_client_engine_callback,1,1); // FIXME xqc_client_engine_callback定时器有问题
    ev_timer_start(ctx->loop,&ctx->ev_engine);
    LOGE("BBB %p",ctx);

    //初始化引擎
    ctx->engine = init_engine(ctx);

    //第三步：初始化h3跟注册应用层回调
    init_alpn_ctx(ctx);

    return ctx;
}

//开始client端
int client_connect(client_ctx_t * client,const char *host ,int port,const char *token,const char* session,const char*transport){
    DEBUG;

    char server_addr[64];
    strcpy(server_addr,host);

    int server_port = port;

    user_conn_t *user_conn = xqc_client_user_conn_create(client,server_addr,server_port);//创建socket链接
    if (user_conn == NULL) {
        LOGE("xqc_client_user_conn_create error\n");
        return -1;
    }

    if(token != NULL){
        int token_len = strlen(token);
        unsigned char token_new[token_len];
        strcpy(token_new,token);
        user_conn->token = token_new;
        user_conn->token_len = token_len;
    }

    //第三步：拥赛控制
    xqc_cong_ctrl_callback_t cong_ctrl;
    uint32_t cong_flags = 0;
    cong_ctrl = xqc_bbr_cb;//bbr拥塞控制算法
    cong_flags = XQC_BBR_FLAG_NONE;

    xqc_conn_settings_t conn_settings = {//链接设置
        .pacing_on  =   0,
        .ping_on    =   0,
        .cong_ctrl_callback = cong_ctrl,
        .cc_params  =   {.customize_on = 1, .init_cwnd = 32, .cc_optimization_flags = cong_flags},
        //.so_sndbuf  =   1024*1024,
        .proto_version = XQC_VERSION_V1,
        .spurious_loss_detect_on = 0,
        .keyupdate_pkt_threshold = 0,
    };


    xqc_conn_ssl_config_t conn_ssl_config;//ssl配置
    memset(&conn_ssl_config, 0, sizeof(conn_ssl_config));

    if(session!=NULL){
        int session_len = strlen(session);
        char session_new[session_len];
        strcpy(session_new,token);
        conn_ssl_config.session_ticket_data= session_new;
        conn_ssl_config.session_ticket_len = session_len;
    }else{
        conn_ssl_config.session_ticket_data = NULL;
        conn_ssl_config.session_ticket_len = NULL;
    }

    if(transport!=NULL){
        int transport_len = strlen(transport);
        char transport_new[transport_len];
        strcpy(transport_new,transport);
        conn_ssl_config.transport_parameter_data = transport_new;
        conn_ssl_config.transport_parameter_data_len = transport_len;
    }else{
        conn_ssl_config.transport_parameter_data = NULL;
        conn_ssl_config.transport_parameter_data_len = NULL;
    }

    const xqc_cid_t *cid;
    cid = xqc_h3_connect(client->engine,
                        &conn_settings,
                        user_conn->token, user_conn->token_len,/**token**/
                        host,
                        0/**1是无密码**/,
                        &conn_ssl_config,
                        user_conn->peer_addr,
                        user_conn->peer_addrlen, user_conn);


    if(cid == NULL){
        LOGE("connect error: cid is NULL");
       return -1;
    }
    memcpy(&user_conn->cid, cid, sizeof(*cid));//保存到user_conn中

    ev_run(client->loop,0);//阻塞
    return 0;
}

//销毁client
int client_destroy(client_ctx_t * client){
    DEBUG;

    xqc_engine_destroy(client->engine);
    free(client);
    return 0;
}