//
// Created by yqy on 2022/3/23.
//

#include "common.h"
#include "xquic_common.h"

#ifndef _Hncluded_XQUIC_APP_PROTO_CALLBACKS
#define _Hncluded_XQUIC_APP_PROTO_CALLBACKS

#ifdef __cplusplus

extern "C"{
#endif

int xqc_client_conn_create_notify(xqc_connection_t *conn, const xqc_cid_t *cid, void *user_data, void *conn_proto_data);

int
xqc_client_conn_close_notify(xqc_connection_t *conn, const xqc_cid_t *cid, void *user_data, void *conn_proto_data);

void
xqc_client_conn_handshake_finished(xqc_connection_t *conn, void *user_data, void *conn_proto_data);

void
xqc_client_conn_ping_acked_notify(xqc_connection_t *conn, const xqc_cid_t *cid, void *ping_user_data, void *user_data, void *conn_proto_data);

int
xqc_client_stream_write_notify(xqc_stream_t *stream, void *user_data);

int
xqc_client_stream_read_notify(xqc_stream_t *stream, void *user_data);

int
xqc_client_stream_close_notify(xqc_stream_t *stream, void *user_data);

#ifdef __cplusplus
}
#endif
#endif