#include "common.h"

#ifndef _Hncluded_XQUIC_H3_CALLBACKS
#define _Hncluded_XQUIC_H3_CALLBACKS

#ifdef __cplusplus

extern "C"{
#endif

int xqc_client_h3_conn_create_notify(xqc_h3_conn_t *conn, const xqc_cid_t *cid, void *user_data);

int xqc_client_h3_conn_close_notify(xqc_h3_conn_t *conn, const xqc_cid_t *cid, void *user_data);

void xqc_client_h3_conn_handshake_finished(xqc_h3_conn_t *h3_conn, void *user_data);

void xqc_client_h3_conn_ping_acked_notify(xqc_h3_conn_t *conn, const xqc_cid_t *cid, void *ping_user_data, void *user_data);

void xqc_client_h3_conn_update_cid_notify(xqc_h3_conn_t *conn, const xqc_cid_t *retire_cid, const xqc_cid_t *new_cid, void *user_data);

int xqc_client_request_close_notify(xqc_h3_request_t *h3_request, void *user_data);

int xqc_client_request_read_notify(xqc_h3_request_t *h3_request, xqc_request_notify_flag_t flag, void *user_data);

int xqc_client_request_write_notify(xqc_h3_request_t *h3_request, void *user_data);

#ifdef __cplusplus
}
#endif
#endif