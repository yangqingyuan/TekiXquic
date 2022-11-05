//
// Created by yqy on 2022/3/23.
//

#include "xquic_common.h"

#ifndef _Hncluded_XQUIC_TRANSPORT_CALLBACKS
#define _Hncluded_XQUIC_TRANSPORT_CALLBACKS

#ifdef __cplusplus

extern "C"{
#endif

void client_save_token(const unsigned char *token, unsigned token_len, void *user_data);

void client_save_session_cb(const char * data, size_t data_len, void *user_data);

void client_save_tp_cb(const char * data, size_t data_len, void * user_data);

int client_cert_verify_cb(const unsigned char **certs,
                          const size_t *cert_len, size_t certs_len, void *conn_user_data);

void client_conn_update_cid_notify(xqc_connection_t *conn, const xqc_cid_t *retire_cid,
                                   const xqc_cid_t *new_cid, void *user_data);

#ifdef __cplusplus
}
#endif
#endif