#include "xquic_client.h"
#include "log.h"

void xqc_keylog_cb(const char *line, void *user_data)
{
    DEBUG;
    client_ctx_t *ctx = (client_ctx_t*)user_data;
}

void xqc_client_write_log(xqc_log_level_t lvl, const void *buf, size_t count, void *engine_user_data)
{
    DEBUG;
    client_ctx_t *ctx = (client_ctx_t*)engine_user_data;
    switch(lvl){
        case XQC_LOG_REPORT:
        case XQC_LOG_FATAL:
        case XQC_LOG_ERROR:
            LOGE("fun:%s,line:%d,log:%s\n",__FUNCTION__,__LINE__,buf);
        break;
        case XQC_LOG_WARN:
            LOGW("fun:%s,line:%d,log:%s\n",__FUNCTION__,__LINE__,buf);
        break;
        case XQC_LOG_INFO:
        case XQC_LOG_STATS:
            LOGI("fun:%s,line:%d,log:%s\n",__FUNCTION__,__LINE__,buf);
        break;
        case XQC_LOG_DEBUG:
            LOGD("fun:%s,line:%d,log:%s\n",__FUNCTION__,__LINE__,buf);
        break;
    }
}

void xqc_client_set_event_timer(xqc_msec_t wake_after, void *user_data)
{
    DEBUG;
    client_ctx_t *ctx = (client_ctx_t *) user_data;
    //printf("xqc_engine_wakeup_after %llu us, now %llu\n", wake_after, now());
    struct timeval tv;
    tv.tv_sec = wake_after / 1000000;
    tv.tv_usec = wake_after % 1000000;
    //event_add(ctx->ev_engine, &tv);

}

ssize_t xqc_client_write_socket(const unsigned char *buf, size_t size,
    const struct sockaddr *peer_addr, socklen_t peer_addrlen, void *user)
{
    DEBUG;
}

void xqc_client_save_token(const unsigned char *token, unsigned token_len, void *user_data)
{
    DEBUG;
}

void save_session_cb(const char * data, size_t data_len, void *user_data)
{
    DEBUG;
}

void save_tp_cb(const char * data, size_t data_len, void * user_data)
{
    DEBUG;
}

int xqc_client_cert_verify(const unsigned char *certs[],
    const size_t cert_len[], size_t certs_len, void *conn_user_data)
{
    /* self-signed cert used in test cases, return >= 0 means success */
    DEBUG;
    return 0;
}


/**
* 初始化引擎
*/
long initEngine(){
    DEBUG;

    //第一步：初始化clientCtx
    client_ctx_t ctx;
    memset(&ctx, 0, sizeof(ctx));

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
        return -1;
    }
    config.cfg_log_level = XQC_LOG_DEBUG;//设置log级别

    //第六部：创建引擎
    ctx.engine = xqc_engine_create(XQC_ENGINE_CLIENT, &config, &engine_ssl_config,&callback, &tcbs, &ctx);

    return &ctx;
}

int startEngine(){
    DEBUG;
}

int destroyEngine(){
    DEBUG;
}