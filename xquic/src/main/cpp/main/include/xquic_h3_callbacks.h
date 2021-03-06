//
// Created by yqy on 2022/3/23.
//

#include "xquic_common.h"

#ifndef _Hncluded_XQUIC_H3_CALLBACKS
#define _Hncluded_XQUIC_H3_CALLBACKS

#ifdef __cplusplus

extern "C"{
#endif

int client_h3_conn_create_notify(xqc_h3_conn_t *conn, const xqc_cid_t *cid, void *user_data);

int client_h3_conn_close_notify(xqc_h3_conn_t *conn, const xqc_cid_t *cid, void *user_data);

void client_h3_conn_handshake_finished(xqc_h3_conn_t *h3_conn, void *user_data);

void client_h3_conn_ping_acked_notify(xqc_h3_conn_t *conn, const xqc_cid_t *cid, void *ping_user_data, void *user_data);

int client_h3_request_create_notify(xqc_h3_request_t *h3_request, void *user_data);

int client_h3_request_close_notify(xqc_h3_request_t *h3_request, void *user_data);

int client_h3_request_read_notify(xqc_h3_request_t *h3_request, xqc_request_notify_flag_t flag, void *user_data);

ssize_t client_h3_request_write_notify(xqc_h3_request_t *h3_request, void *user_data);

#ifdef __cplusplus
}
#endif
#endif