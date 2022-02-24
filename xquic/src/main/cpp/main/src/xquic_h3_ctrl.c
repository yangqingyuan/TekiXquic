#include "xquic_h3_ctrl.h"
#include "xquic_h3_callbacks.h"

int xqc_client_h3_init(client_ctx_t* ctx){
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
    return ret;
}

int xqc_client_h3_conn(client_ctx_t* ctx,char *server_addr,xqc_conn_settings_t *conn_settings,xqc_conn_ssl_config_t *conn_ssl_config){
    user_conn_t *user_conn = ctx->user_conn;
    const xqc_cid_t *cid;
    cid = xqc_h3_connect(ctx->engine,
                            conn_settings,
                            user_conn->token, user_conn->token_len,/**token**/
                            server_addr,
                            0/**1是无密码**/,
                            conn_ssl_config,
                            user_conn->peer_addr,
                            user_conn->peer_addrlen, user_conn);
    if(cid == NULL){
      LOGE("connect error: cid is NULL");
     return -1;
    }
    memcpy(&user_conn->cid, cid, sizeof(*cid));//保存到user_conn中
    return 0;
}

