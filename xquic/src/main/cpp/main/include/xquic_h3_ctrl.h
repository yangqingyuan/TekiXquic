#include "common.h"

#ifndef _Hncluded_XQUIC_H3_CTRL
#define _Hncluded_XQUIC_H3_CTRL

#ifdef __cplusplus

extern "C"{
#endif

int xqc_client_h3_init(client_ctx_t* ctx);
int xqc_client_h3_conn(client_ctx_t* ctx,char *server_addr,xqc_conn_settings_t *conn_settings,xqc_conn_ssl_config_t *conn_ssl_config);
int xqu_client_h3_send(client_ctx_t* ctx);

#ifdef __cplusplus
}
#endif
#endif