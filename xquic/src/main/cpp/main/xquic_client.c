#include "xquic_client.h"
#include "xquic_engine_callbacks.h"
#include "xquic_transport_callbacks.h"
#include "xquic_h3_callbacks.h"
#include "xquic_app_proto_callbacks.h"
#include "xquic_socket.h"

/**
* 引擎超时
*/
static void xqc_client_engine_time_out(struct ev_loop *main_loop,ev_timer*time_w,int what)
{
    LOGE("timer wakeup %f\n", time_w->repeat);
    //if(time_w->repeat>0){
        client_ctx_t *ctx = (client_ctx_t *) ev_userdata(main_loop);
        LOGE("BBB %p",ctx);
        //xqc_engine_main_logic(ctx->engine);
    //}
}

/**
* 初始化引擎
*/
xqc_engine_t* init_engine(client_ctx_t * client){
    DEBUG;

    //初始化ssl_config
    xqc_engine_ssl_config_t  engine_ssl_config;
    memset(&engine_ssl_config, 0, sizeof(engine_ssl_config));
    engine_ssl_config.ciphers = XQC_TLS_CIPHERS;//加密算法，用默认
    engine_ssl_config.groups = XQC_TLS_GROUPS;//加密组，默认就行

    // 初始化引擎回调跟传输回调
    xqc_engine_callback_t callback = {//注意：回调定义在engine_callback.h中
            .set_event_timer = xqc_client_set_event_timer,/* call xqc_engine_main_logic when the timer expires */
            .log_callbacks = {
                .xqc_log_write_err = xqc_client_write_log,
                .xqc_log_write_stat = xqc_client_write_log,
            },
            .keylog_cb = xqc_keylog_cb,
        };

    xqc_transport_callbacks_t tcbs = {//注意：回调定义在transport_callback.h中
        .write_socket = xqc_client_write_socket,
        .save_token = xqc_client_save_token,
        .save_session_cb = save_session_cb,
        .save_tp_cb = save_tp_cb,
        .cert_verify_cb = xqc_client_cert_verify,
    };

    //引擎配置
    xqc_config_t config;
    if (xqc_engine_get_default_config(&config, XQC_ENGINE_CLIENT) < 0) {
        LOGE("get default config error fun:%s,line:%d \n",__FUNCTION__,__LINE__);
        return NULL;
    }
    config.cfg_log_level = XQC_LOG_DEBUG;//设置log级别

    client->ev_engine.data = client;
    ev_timer_init(&client->ev_engine,xqc_client_engine_time_out,0,0); // FIXME xqc_client_engine_callback定时器有问题
    ev_timer_start(loop,&client->ev_engine);

    //创建引擎
    return xqc_engine_create(XQC_ENGINE_CLIENT, &config, &engine_ssl_config,&callback, &tcbs,client);
}

/**
* 初始化H3跟应用层注册
*/
int init_alpn_ctx(client_ctx_t* ctx){
    DEBUG;
    xqc_h3_callbacks_t h3_cbs = {//注意：回调定义在H3_callback.h中
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
    xqc_app_proto_callbacks_t ap_cbs = {//注意：回调定义在app_proto_callbacks.h中
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

//初始化client端
client_ctx_t * client_init(){
    DEBUG;

    if(loop == NULL){
        loop = EV_DEFAULT;
    }

    //初始化clientCtx
    client_ctx_t *ctx = calloc(1, sizeof(client_ctx_t));

    //第一步：初始化引擎
    ctx->engine = init_engine(ctx);

    //第二步：初始化h3跟注册应用层回调
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

    client->user_conn = user_conn;

    if(token != NULL){
        int token_len = strlen(token);
        char token_new[token_len];
        strcpy(token_new,token);
        user_conn->token = (unsigned char *)token_new;
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
        conn_ssl_config.session_ticket_len = 0;
    }

    if(transport!=NULL){
        int transport_len = strlen(transport);
        char transport_new[transport_len];
        strcpy(transport_new,transport);
        conn_ssl_config.transport_parameter_data = transport_new;
        conn_ssl_config.transport_parameter_data_len = transport_len;
    }else{
        conn_ssl_config.transport_parameter_data = NULL;
        conn_ssl_config.transport_parameter_data_len = 0;
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

    return 0;
}


//链接完毕后开始
void client_start(client_ctx_t * client){
    DEBUG;
    ev_run(loop,0);//阻塞
    LOGE("ev_run end");
}

//H3的方式发送内容
int client_send_h3(client_ctx_t * client,xqc_http_headers_t* headers,const char *body){
    return 0;
}

//HQ的方式返送内容
int client_send_hq(client_ctx_t * client,const char *body){
    return 0;
}


//销毁client
int client_destroy(client_ctx_t * client){
    DEBUG;

    xqc_engine_destroy(client->engine);
    free(client);
    return 0;
}