#include "xquic_app_proto_callbacks.h"
#include "xquic_hq_ctrl.h"

int xqc_client_hq_init(client_ctx_t* ctx){
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

int xqc_client_hq_conn(client_ctx_t* ctx){

}