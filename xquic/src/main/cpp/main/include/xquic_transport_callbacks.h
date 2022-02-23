#include "common.h"

#ifndef _Hncluded_XQUIC_TRANSPORT_CALLBACKS
#define _Hncluded_XQUIC_TRANSPORT_CALLBACKS

#ifdef __cplusplus

extern "C"{
#endif

ssize_t xqc_client_write_socket(const unsigned char *buf, size_t size,
    const struct sockaddr *peer_addr, socklen_t peer_addrlen, void *user);

void xqc_client_save_token(const unsigned char *token, unsigned token_len, void *user_data);

void save_session_cb(const char * data, size_t data_len, void *user_data);

void save_tp_cb(const char * data, size_t data_len, void * user_data);

int xqc_client_cert_verify(const unsigned char *certs[],
    const size_t cert_len[], size_t certs_len, void *conn_user_data);

#ifdef __cplusplus
}
#endif
#endif