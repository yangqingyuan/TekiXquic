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

    //第三步：拥赛控制
    xqc_cong_ctrl_callback_t cong_ctrl;
    uint32_t cong_flags = 0;
    cong_ctrl = xqc_bbr_cb;//bbr拥塞控制算法
    cong_flags = XQC_BBR_FLAG_NONE;

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


//初始化client端
client_ctx_t * client_init(){
    DEBUG;
    //第一步：初始化clientCtx
    client_ctx_t *ctx = calloc(1, sizeof(client_ctx_t));
    //memset(ctx, 0, sizeof(ctx));

    //第二步：初始化引擎
    ctx->engine = init_engine(ctx);

    //第三步：初始化h3跟注册应用层回调
    init_alpn_ctx(ctx);

    return ctx;
}

//开始client端
int client_connect(client_ctx_t * client,const char *host ,int port,const char *token,const char* session){
    DEBUG;

    return 0;
}

//销毁client
int client_destroy(client_ctx_t * client){
    DEBUG;

    xqc_engine_destroy(client->engine);
    free(client);
    return 0;
}